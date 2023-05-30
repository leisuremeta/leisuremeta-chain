package io.leisuremeta.chain
package bulkinsert

import java.time.temporal.ChronoUnit

import cats.data.{EitherT, StateT}
import cats.effect.Async
import cats.syntax.all.*

import api.model.{AccountData, PublicKeySummary, Signed, Transaction, TransactionWithResult}
import api.model.TransactionWithResult.ops.*
import lib.crypto.Hash.ops.*
import lib.datatype.{BigNat, Utf8}
import lib.merkle.MerkleTrieState
import node.dapp.{PlayNommDApp, PlayNommDAppFailure, PlayNommState}
import node.dapp.submodule.*
import node.repository.TransactionRepository

object RecoverTx:

  def apply[F[_]: Async: TransactionRepository: PlayNommState: InvalidTxLogger](
      e: PlayNommDAppFailure,
      ms: MerkleTrieState,
      signedTx: Signed.Tx,
  ): EitherT[F, PlayNommDAppFailure, (MerkleTrieState, TransactionWithResult)] =
    EitherT
      .pure[F, PlayNommDAppFailure]:
        scribe.error(s"Recovering tx: $signedTx")
        scribe.error(s"Error: $e")
      .flatMap: _ =>
        signedTx.value match
          case ca: Transaction.AccountTx.CreateAccount =>
            val program = for
              _ <-
                if signedTx.sig.account === ca.account then
                  for
                    initialPKS <- PlayNommDAppAccount.getPKS(signedTx.sig, ca)
                    keyInfo = PublicKeySummary.Info(
                      addedAt = ca.createdAt,
                      description =
                        Utf8.unsafeFrom(s"automatically added at account creation"),
                      expiresAt = Some(ca.createdAt.plus(40, ChronoUnit.DAYS)),
                    )
                    _ <- PlayNommState[F].account.key
                      .put((ca.account, initialPKS), keyInfo)
                      .mapK(PlayNommDAppFailure.mapInternal {
                        s"Fail to put account key ${ca.account}"
                      })
                  yield ()
                else
                  StateT.empty[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, Unit]
              accountData = AccountData(
                guardian = ca.guardian,
                ethAddress = ca.ethAddress,
                lastChecked = ca.createdAt,
              )
              _ <- PlayNommState[F].account.name
                .put(ca.account, accountData)
                .mapK(PlayNommDAppFailure.mapInternal {
                  s"Fail to put account ${ca.account}"
                })
              _ <- ca.ethAddress
                .fold(StateT.empty[EitherT[F, String, *], MerkleTrieState, Unit]): ethAddress =>
                  PlayNommState[F].account.eth.put(ethAddress, ca.account)
                .mapK:
                  PlayNommDAppFailure.mapInternal(s"Fail to update eth address ${ca.ethAddress}")
            yield TransactionWithResult(signedTx)(None)

            program
              .run(ms)
              .leftMap: msg =>
                PlayNommDAppFailure.internal(s"Fail to recover error: $msg")
          case tx: Transaction.AccountTx.AddPublicKeySummaries =>
            val program = for
              account <- PlayNommState[F].account.name.get(tx.account).map: account =>
                scribe.info(s"Account Data: $account")
                account
              _ <- StateT.liftF:
                EitherT.leftT[F, (MerkleTrieState, TransactionWithResult)]:
                  s"Not recovered yet: ${e.msg}"
            yield TransactionWithResult(signedTx)(None)

            program
              .run(ms)
              .leftMap: msg =>
                PlayNommDAppFailure.internal(s"Fail to recover error: $msg")

          case tf: Transaction.TokenTx.TransferFungibleToken =>
            val sig = signedTx.sig
            val tx = signedTx.value
            val program = for
              _        <- PlayNommDAppAccount.verifySignature(sig, tx)
              tokenDef <- PlayNommDAppToken.getTokenDefinition(tf.tokenDefinitionId)
              inputAmount <- PlayNommDAppToken.getFungibleBalanceTotalAmounts(
                tf.inputs.map(_.toResultHashValue),
                sig.account,
              )
              outputAmount = tf.outputs.values.foldLeft(BigNat.Zero)(BigNat.add)
              diffBigInt = inputAmount.toBigInt - outputAmount.toBigInt
              _ <- StateT.liftF:
                if diffBigInt < 0 then
                  EitherT.right:
                    InvalidTxLogger[F].log:
                      InvalidTx(
                        signer = sig.account,
                        reason = InvalidReason.OutputMoreThanInput,
                        amountToBurn = BigNat.unsafeFromBigInt(diffBigInt.abs),
                        tx = tf,
                      )
                else EitherT.pure(())
              txWithResult = TransactionWithResult(Signed(sig, tf))(None)
              txHash       = txWithResult.toHash
              invalidInputs <- PlayNommDAppToken.removeInputUtxos(
                sig.account,
                tf.inputs.map(_.toResultHashValue),
                tf.tokenDefinitionId,
              )
              _ <- StateT.liftF:
                if invalidInputs.isEmpty then EitherT.pure(()) else
                  invalidInputs
                    .traverse(TransactionRepository[F].get)
                    .leftMap(e => PlayNommDAppFailure.internal(s"Fail to get tx: $e"))
                    .semiflatMap: txOptions =>
                      val sum = txOptions
                        .map: txOption =>
                          txOption.fold(BigNat.Zero)(PlayNommDAppToken.tokenBalanceAmount(sig.account))
                        .foldLeft(BigNat.Zero)(BigNat.add)
                      InvalidTxLogger[F].log:
                        InvalidTx(
                          signer = sig.account,
                          reason = InvalidReason.InputAlreadyUsed,
                          amountToBurn = sum,
                          tx = tf,
                        )
              _ <- tf.outputs.toSeq.traverse:
                case (account, _) =>
                  PlayNommDAppToken.putBalance(account, tf.tokenDefinitionId, txHash)
              totalAmount <- fromEitherInternal:
                BigNat.fromBigInt(tokenDef.totalAmount.toBigInt - diffBigInt)
              _ <- PlayNommDAppToken.putTokenDefinition(
                tf.tokenDefinitionId,
                tokenDef.copy(totalAmount = totalAmount),
              )
            yield txWithResult

            program
              .run(ms)
              .leftMap: msg =>
                PlayNommDAppFailure.internal(s"Fail to recover error: $msg")

          case ef: Transaction.TokenTx.EntrustFungibleToken =>
            val sig = signedTx.sig
            val tx = signedTx.value
            val program = for
              _        <- PlayNommDAppAccount.verifySignature(sig, tx)
              tokenDef <- PlayNommDAppToken.getTokenDefinition(ef.definitionId)
              inputAmount <- PlayNommDAppToken.getFungibleBalanceTotalAmounts(
                ef.inputs.map(_.toResultHashValue),
                sig.account,
              )
              diffBigInt = inputAmount.toBigInt - ef.amount.toBigInt
              diff <- StateT.liftF:
                if diffBigInt < 0 then
                  EitherT.right:
                    InvalidTxLogger[F].log:
                      InvalidTx(
                        signer = sig.account,
                        reason = InvalidReason.OutputMoreThanInput,
                        amountToBurn = BigNat.unsafeFromBigInt(diffBigInt.abs),
                        tx = ef,
                      )
                    .as(BigNat.Zero)
                else EitherT
                  .fromEither(BigNat.fromBigInt(diffBigInt))
                  .leftMap: msg =>
                    PlayNommDAppFailure.internal(s"Fail to convert diff to BigNat: $msg")
              result       = Transaction.TokenTx.EntrustFungibleTokenResult(diff)
              txWithResult = TransactionWithResult(Signed(sig, ef))(Some(result))
              txHash       = txWithResult.toHash
              invalidInputs <- PlayNommDAppToken.removeInputUtxos(
                sig.account,
                ef.inputs.map(_.toResultHashValue),
                ef.definitionId,
              )
              _ <- StateT.liftF:
                if invalidInputs.isEmpty then EitherT.pure(()) else
                  invalidInputs
                    .traverse(TransactionRepository[F].get)
                    .leftMap(e => PlayNommDAppFailure.internal(s"Fail to get tx: $e"))
                    .semiflatMap: txOptions =>
                      val sum = txOptions
                        .map: txOption =>
                          txOption.fold(BigNat.Zero)(PlayNommDAppToken.tokenBalanceAmount(sig.account))
                        .foldLeft(BigNat.Zero)(BigNat.add)
                      InvalidTxLogger[F].log:
                        InvalidTx(
                          signer = sig.account,
                          reason = InvalidReason.InputAlreadyUsed,
                          amountToBurn = sum,
                          tx = ef,
                        )
              _ <- PlayNommState[F].token.entrustFungibleBalance
                .put((sig.account, ef.to, ef.definitionId, txHash), ())
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to put entrust fungible balance of (${sig.account}, ${ef.to}, ${ef.definitionId}, ${txHash})"
              _ <- PlayNommDAppToken.putBalance(sig.account, ef.definitionId, txHash)
            yield txWithResult

            program
              .run(ms)
              .leftMap: msg =>
                PlayNommDAppFailure.internal(s"Fail to recover error: $msg")

          case _ =>
            PlayNommDApp[F](signedTx).run(ms)
