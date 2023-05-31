package io.leisuremeta.chain
package bulkinsert

import java.time.temporal.ChronoUnit

import cats.data.{EitherT, StateT}
import cats.effect.Async
import cats.syntax.all.*

import api.model.{
  AccountData,
  PublicKeySummary,
  Signed,
  Transaction,
  TransactionWithResult,
}
import api.model.TransactionWithResult.ops.*
import lib.codec.byte.ByteEncoder.ops.*
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
  ): EitherT[
    F,
    PlayNommDAppFailure,
    (MerkleTrieState, Option[TransactionWithResult]),
  ] =
    EitherT
      .pure[F, PlayNommDAppFailure]:
        scribe.error(s"Recovering tx: $signedTx")
        scribe.error(s"Error: $e")
      .flatMap: _ =>
        val sig = signedTx.sig
        val tx  = signedTx.value
        tx match
          case ca: Transaction.AccountTx.CreateAccount =>
            val program = for
              _ <-
                if signedTx.sig.account === ca.account then
                  for
                    initialPKS <- PlayNommDAppAccount.getPKS(signedTx.sig, ca)
                    keyInfo = PublicKeySummary.Info(
                      addedAt = ca.createdAt,
                      description = Utf8
                        .unsafeFrom(s"automatically added at account creation"),
                      expiresAt = Some(ca.createdAt.plus(40, ChronoUnit.DAYS)),
                    )
                    _ <- PlayNommState[F].account.key
                      .put((ca.account, initialPKS), keyInfo)
                      .mapK(PlayNommDAppFailure.mapInternal {
                        s"Fail to put account key ${ca.account}"
                      })
                  yield ()
                else
                  StateT.empty[EitherT[
                    F,
                    PlayNommDAppFailure,
                    *,
                  ], MerkleTrieState, Unit]
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
                .fold(
                  StateT.empty[EitherT[F, String, *], MerkleTrieState, Unit],
                ): ethAddress =>
                  PlayNommState[F].account.eth.put(ethAddress, ca.account)
                .mapK:
                  PlayNommDAppFailure.mapInternal(
                    s"Fail to update eth address ${ca.ethAddress}",
                  )
            yield TransactionWithResult(signedTx)(None)

            program
              .run(ms)
              .map: (ms, txWithResult) =>
                (ms, Option(txWithResult))
              .leftMap: msg =>
                PlayNommDAppFailure.internal(s"Fail to recover error: $msg")

          case ua: Transaction.AccountTx.UpdateAccount =>
            val program = for
              accountDataOption <- PlayNommDAppAccount.getAccountInfo(
                ua.account,
              )
              accountData <- fromOption(
                accountDataOption,
                s"${ua.account} does not exists",
              )
              _ <- checkExternal(
                sig.account == ua.account ||
                  Some(sig.account) == accountData.guardian,
                s"Signer ${sig.account} is neither ${ua.account} nor its guardian",
              )
              _ <- PlayNommState[F].account.name
                .put(
                  ua.account,
                  accountData
                    .copy(guardian = ua.guardian, ethAddress = ua.ethAddress),
                )
                .mapK(PlayNommDAppFailure.mapInternal {
                  s"Fail to put account ${ua.account}"
                })
              _ <- ua.ethAddress
                .fold(
                  StateT.empty[EitherT[F, String, *], MerkleTrieState, Unit],
                ): ethAddress =>
                  PlayNommState[F].account.eth.put(ethAddress, ua.account)
                .mapK:
                  PlayNommDAppFailure.mapInternal(
                    s"Fail to update eth address ${ua.ethAddress}",
                  )
            yield TransactionWithResult(Signed(sig, ua))(None)

            program
              .run(ms)
              .map: (ms, txWithResult) =>
                (ms, Option(txWithResult))
              .leftMap: msg =>
                PlayNommDAppFailure.internal(s"Fail to recover error: $msg")

          case ap: Transaction.AccountTx.AddPublicKeySummaries =>
            val program = for
              account <- PlayNommState[F].account.name
                .get(ap.account)
                .map: account =>
                  scribe.info(s"Account Data: $account")
                  account
              _ <- StateT.liftF:
                EitherT.leftT[F, (MerkleTrieState, TransactionWithResult)]:
                  s"Not recovered yet: ${e.msg}"
            yield TransactionWithResult(signedTx)(None)

            program
              .run(ms)
              .map: (ms, txWithResult) =>
                (ms, Option(txWithResult))
              .leftMap: msg =>
                PlayNommDAppFailure.internal(s"Fail to recover error: $msg")

          case tf: Transaction.TokenTx.TransferFungibleToken =>
            val sig = signedTx.sig
            val tx  = signedTx.value
            val program = for
              _ <- PlayNommDAppAccount.verifySignature(sig, tx)
              tokenDef <- PlayNommDAppToken.getTokenDefinition(
                tf.tokenDefinitionId,
              )
              inputAmount <- PlayNommDAppToken.getFungibleBalanceTotalAmounts(
                tf.inputs.map(_.toResultHashValue),
                sig.account,
              )
              outputAmount = tf.outputs.values.foldLeft(BigNat.Zero)(BigNat.add)
              diffBigInt   = inputAmount.toBigInt - outputAmount.toBigInt
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
                if invalidInputs.isEmpty then EitherT.pure(())
                else
                  invalidInputs
                    .traverse(TransactionRepository[F].get)
                    .leftMap(e =>
                      PlayNommDAppFailure.internal(s"Fail to get tx: $e"),
                    )
                    .semiflatMap: txOptions =>
                      val sum = txOptions
                        .map: txOption =>
                          txOption.fold(BigNat.Zero)(
                            PlayNommDAppToken.tokenBalanceAmount(sig.account),
                          )
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
                  PlayNommDAppToken.putBalance(
                    account,
                    tf.tokenDefinitionId,
                    txHash,
                  )
              totalAmount <- fromEitherInternal:
                BigNat.fromBigInt(tokenDef.totalAmount.toBigInt - diffBigInt)
              _ <- PlayNommDAppToken.putTokenDefinition(
                tf.tokenDefinitionId,
                tokenDef.copy(totalAmount = totalAmount),
              )
            yield txWithResult

            program
              .run(ms)
              .map: (ms, txWithResult) =>
                (ms, Option(txWithResult))
              .leftMap: msg =>
                PlayNommDAppFailure.internal(s"Fail to recover error: $msg")

          case ef: Transaction.TokenTx.EntrustFungibleToken =>
            val sig = signedTx.sig
            val tx  = signedTx.value
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
                    InvalidTxLogger[F]
                      .log:
                        InvalidTx(
                          signer = sig.account,
                          reason = InvalidReason.OutputMoreThanInput,
                          amountToBurn =
                            BigNat.unsafeFromBigInt(diffBigInt.abs),
                          tx = ef,
                        )
                      .as(BigNat.Zero)
                else
                  EitherT
                    .fromEither(BigNat.fromBigInt(diffBigInt))
                    .leftMap: msg =>
                      PlayNommDAppFailure.internal(
                        s"Fail to convert diff to BigNat: $msg",
                      )
              result = Transaction.TokenTx.EntrustFungibleTokenResult(diff)
              txWithResult = TransactionWithResult(Signed(sig, ef))(
                Some(result),
              )
              txHash = txWithResult.toHash
              invalidInputs <- PlayNommDAppToken.removeInputUtxos(
                sig.account,
                ef.inputs.map(_.toResultHashValue),
                ef.definitionId,
              )
              _ <- StateT.liftF:
                if invalidInputs.isEmpty then EitherT.pure(())
                else
                  invalidInputs
                    .traverse(TransactionRepository[F].get)
                    .leftMap(e =>
                      PlayNommDAppFailure.internal(s"Fail to get tx: $e"),
                    )
                    .semiflatMap: txOptions =>
                      val sum = txOptions
                        .map: txOption =>
                          txOption.fold(BigNat.Zero)(
                            PlayNommDAppToken.tokenBalanceAmount(sig.account),
                          )
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
              _ <- PlayNommDAppToken.putBalance(
                sig.account,
                ef.definitionId,
                txHash,
              )
            yield txWithResult

            program
              .run(ms)
              .map: (ms, txWithResult) =>
                (ms, Option(txWithResult))
              .leftMap: msg =>
                PlayNommDAppFailure.internal(s"Fail to recover error: $msg")

          case ef: Transaction.TokenTx.EntrustNFT =>
            val program = for
              _ <- PlayNommDAppAccount.verifySignature(sig, tx)
              txWithResult = TransactionWithResult(Signed(sig, ef))(None)
              txHash       = txWithResult.toHash
              inputTxHashes <- PlayNommState[F].token.nftBalance
                .from((sig.account, ef.tokenId).toBytes)
                .flatMapF: stream =>
                  stream.map(_._1._3).compile.toList
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to get nft balance stream of (${sig.account}, ${ef.tokenId})"
              inputTxHash <- fromEitherExternal:
                inputTxHashes.headOption.toRight:
                  s"No NFT Balance: ${sig.account}, ${ef.tokenId}"
              utxoKey = (sig.account, ef.tokenId, inputTxHash)
              isRemoveSuccessful <- PlayNommState[F].token.nftBalance
                .remove(utxoKey)
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to remove nft balance of $utxoKey"
              _ <- checkExternal(
                isRemoveSuccessful,
                s"No NFT Balance: ${utxoKey}",
              )
              newUtxoKey = (sig.account, ef.to, ef.tokenId, txHash)
              _ <- PlayNommState[F].token.entrustNftBalance
                .put(newUtxoKey, ())
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to put entrust nft balance of $newUtxoKey"
            yield txWithResult

            program
              .run(ms)
              .map: (ms, txWithResult) =>
                (ms, Option(txWithResult))
              .leftMap: msg =>
                PlayNommDAppFailure.internal(s"Fail to recover error: $msg")

          case de: Transaction.TokenTx.DisposeEntrustedNFT =>
            val program = for
              _ <- PlayNommDAppAccount.verifySignature(sig, tx)
              entrustedNFT <- PlayNommDAppToken.getEntrustedNFT(
                de.input.toResultHashValue,
                sig.account,
              )
              (fromAccount, entrustTx) = entrustedNFT
              txWithResult = TransactionWithResult(Signed(sig, de))(None)
              txHash       = txWithResult.toHash
              utxoKey = (
                fromAccount,
                sig.account,
                de.tokenId,
                de.input.toResultHashValue,
              )
              isRemoveSuccessful <- PlayNommState[F].token.entrustNftBalance
                .remove(utxoKey)
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to remove entrust nft balance of $utxoKey"
              txWithResultOption =
                if isRemoveSuccessful then Some(txWithResult)
                else
                  scribe.error(s"No entrust nft balance of $utxoKey in tx $tx")
                  None

              _ <-
                if isRemoveSuccessful then
                  unit[F].flatMapF: _ =>
                    EitherT.liftF:
                      InvalidTxLogger[F].log:
                        InvalidTx(
                          signer = sig.account,
                          reason = InvalidReason.InputAlreadyUsed,
                          amountToBurn = BigNat.Zero,
                          tx = de,
                        )
                else
                  val newUtxoKey = (
                    de.output.getOrElse(fromAccount),
                    de.tokenId,
                    txHash,
                  )
                  for
                    _ <- PlayNommState[F].token.nftBalance
                      .put(newUtxoKey, ())
                      .mapK:
                        PlayNommDAppFailure.mapInternal:
                          s"Fail to put nft balance of $newUtxoKey"
                    _ <- de.output.fold(unit): toAddress =>
                      for
                        nftStateOption <- PlayNommState[F].token.nftState
                          .get(de.tokenId)
                          .mapK:
                            PlayNommDAppFailure.mapInternal:
                              s"Fail to get nft state of ${de.tokenId}"
                        nftState <- fromOption(
                          nftStateOption,
                          s"Empty NFT State: ${de.tokenId}",
                        )
                        nftState1 = nftState.copy(currentOwner = toAddress)
                        _ <- PlayNommState[F].token.nftState
                          .put(de.tokenId, nftState1)
                          .mapK:
                            PlayNommDAppFailure.mapInternal:
                              s"Fail to put nft state of ${de.tokenId}"
                      yield ()
                  yield ()
            yield txWithResultOption

            program
              .run(ms)
              .leftMap: msg =>
                PlayNommDAppFailure.internal(s"Fail to recover error: $msg")

          case _ =>
            PlayNommDApp[F](signedTx)
              .run(ms)
              .map: (ms, txWithResult) =>
                (ms, Option(txWithResult))
