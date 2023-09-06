package io.leisuremeta.chain
package bulkinsert

import java.time.temporal.ChronoUnit

import cats.data.{EitherT, OptionT, StateT}
import cats.effect.Async
import cats.syntax.all.*

import api.model.{
  Account,
  AccountData,
  PublicKeySummary,
  Signed,
  Transaction,
  TransactionWithResult,
}
import api.model.TransactionWithResult.ops.*
import api.model.account.{ExternalChain, ExternalChainAddress}
import api.model.token.TokenId
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
              accountInfoOption <- PlayNommDAppAccount.getAccountInfo(ca.account)
              _ <- checkExternal(
                accountInfoOption.isEmpty,
                s"${ca.account} already exists",
              )
              _ <- checkExternal(
                sig.account == ca.account ||
                  Some(sig.account) == ca.guardian,
                s"Signer ${sig.account} is neither ${ca.account} nor its guardian",
              )
              initialPKS <- PlayNommDAppAccount.getPKS(sig, ca)
              keyInfo = PublicKeySummary.Info(
                addedAt = ca.createdAt,
                description =
                  Utf8.unsafeFrom(s"automatically added at account creation"),
                expiresAt = Some(ca.createdAt.plus(40, ChronoUnit.DAYS)),
              )
              _ <-
                if Option(sig.account) === ca.guardian then unit
                else
                  PlayNommState[F].account.key
                    .put((ca.account, initialPKS), keyInfo)
                    .mapK:
                      PlayNommDAppFailure.mapInternal:
                        s"Fail to put account key ${ca.account}"
              accountData = AccountData(
                guardian = ca.guardian,
                externalChainAddresses = ca.ethAddress.fold(Map.empty): ethAddress =>
                  Map(ExternalChain.ETH -> ExternalChainAddress(ethAddress.utf8)),
                lastChecked = ca.createdAt,
                memo = None,
              )
              _ <- PlayNommState[F].account.name
                .put(ca.account, accountData)
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to put account ${ca.account}"
              _ <- ca.ethAddress.fold(unit): ethAddress =>
                PlayNommState[F].account.externalChainAddresses
                  .put(
                    (ExternalChain.ETH, ExternalChainAddress(ethAddress.utf8)),
                    ca.account,
                  )
                  .mapK:
                    PlayNommDAppFailure.mapInternal:
                      s"Fail to update eth address ${ca.ethAddress}"
            yield TransactionWithResult(Signed(sig, ca))(None)

            program
              .run(ms)
              .map: (ms, txWithResult) =>
                (ms, Option(txWithResult))
              .leftMap: msg =>
                PlayNommDAppFailure.internal(s"Fail to recover error: $msg")

          case ua: Transaction.AccountTx.UpdateAccount =>
            val program = for
              accountDataOption <- PlayNommDAppAccount.getAccountInfo(ua.account)
              accountData <- fromOption(
                accountDataOption,
                s"${ua.account} does not exists",
              )
              _ <- checkExternal(
                sig.account == ua.account ||
                  Some(sig.account) == accountData.guardian,
                s"Signer ${sig.account} is neither ${ua.account} nor its guardian",
              )
              accountData1 = accountData.copy(
                guardian = ua.guardian,
                externalChainAddresses = ua.ethAddress.fold(Map.empty): ethAddress =>
                  Map(ExternalChain.ETH -> ExternalChainAddress(ethAddress.utf8)),
                lastChecked = ua.createdAt,
                memo = None,
              )
              _ <- PlayNommState[F].account.name
                .put(ua.account, accountData1)
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to put account ${ua.account}"
              _ <- ua.ethAddress.fold(unit): ethAddress =>
                PlayNommState[F].account.externalChainAddresses
                  .put(
                    (ExternalChain.ETH, ExternalChainAddress(ethAddress.utf8)),
                    ua.account,
                  )
                  .mapK:
                    PlayNommDAppFailure.mapInternal:
                      s"Fail to update eth address ${ua.ethAddress}"
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

          case tn: Transaction.TokenTx.TransferNFT =>
            val sig          = signedTx.sig
            val txWithResult = TransactionWithResult(Signed(sig, tn))(None)
            val txHash       = txWithResult.toHash
            val utxoKey = (sig.account, tn.tokenId, tn.input.toResultHashValue)

            val program = for
              isRemoveSuccessful <- PlayNommState[F].token.nftBalance
                .remove(utxoKey)
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to remove nft balance of $utxoKey"
              _ <-
                if isRemoveSuccessful then StateT.liftF(EitherT.pure(()))
                else if sig.account === Account(Utf8.unsafeFrom("playnomm"))
                then removePreviousNftBalance(tn.tokenId)
                else
                  StateT.liftF:
                    EitherT.right:
                      InvalidTxLogger[F].log:
                        InvalidTx(
                          signer = sig.account,
                          reason = InvalidReason.BalanceNotExist,
                          amountToBurn = BigNat.Zero,
                          tx = tn,
                          wrongNftInput = Some(tn.tokenId),
                        )
              newUtxoKey = (tn.output, tn.tokenId, txHash)
              _ <- PlayNommState[F].token.nftBalance
                .put(newUtxoKey, ())
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to put nft balance of $newUtxoKey"
              nftStateOption <- PlayNommState[F].token.nftState
                .get(tn.tokenId)
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to get nft state of ${tn.tokenId}"
              nftState <- fromOption(
                nftStateOption,
                s"Empty NFT State: ${tn.tokenId}",
              )
              nftState1 = nftState.copy(currentOwner = tn.output)
              _ <- PlayNommState[F].token.nftState
                .put(tn.tokenId, nftState1)
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to put nft state of ${tn.tokenId}"
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
                .streamFrom((sig.account, ef.tokenId).toBytes)
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
              _ <- StateT.liftF:
                if !isRemoveSuccessful then
                  EitherT.right:
                    InvalidTxLogger[F].log:
                      InvalidTx(
                        signer = sig.account,
                        reason = InvalidReason.BalanceNotExist,
                        amountToBurn = BigNat.Zero,
                        tx = ef,
                        wrongNftInput = Some(ef.tokenId),
                      )
                else EitherT.pure(())
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
              fromAccount <- PlayNommDAppToken
                .getEntrustedNFT(
                  de.input.toResultHashValue,
                  sig.account,
                )
                .map(_._1)
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
              _ <-
                if isRemoveSuccessful then unit[F]
                else
                  removePreviousNftBalance[F](de.tokenId) *> StateT.liftF:
                    EitherT.right:
                      InvalidTxLogger[F].log:
                        InvalidTx(
                          signer = sig.account,
                          reason = InvalidReason.BalanceNotExist,
                          amountToBurn = BigNat.Zero,
                          tx = de,
                          wrongNftInput = Some(de.tokenId),
                        )
              txWithResult = TransactionWithResult(Signed(sig, de))(None)
              txHash       = txWithResult.toHash
              toAccount = de.output.getOrElse(fromAccount)
              newUtxoKey = (toAccount, de.tokenId, txHash)
              _ <- PlayNommState[F].token.nftBalance
                .put(newUtxoKey, ())
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to put nft balance of $newUtxoKey"
              nftStateOption <- PlayNommState[F].token.nftState
                .get(de.tokenId)
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to get nft state of ${de.tokenId}"
              nftState <- fromOption(
                nftStateOption,
                s"Empty NFT State: ${de.tokenId}",
              )
              nftState1 = nftState.copy(currentOwner = toAccount)
              _ <- PlayNommState[F].token.nftState
                .put(de.tokenId, nftState1)
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to put nft state of ${de.tokenId}"
            yield Option(txWithResult)

            program
              .run(ms)
              .leftMap: msg =>
                PlayNommDAppFailure.internal(s"Fail to recover error: $msg")

          case tf: Transaction.RewardTx.OfferReward =>
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

          case _ =>
            PlayNommDApp[F](signedTx)
              .run(ms)
              .map: (ms, txWithResult) =>
                (ms, Option(txWithResult))

  def removePreviousNftBalance[F[_]: Async: PlayNommState: TransactionRepository: InvalidTxLogger](
      tokenId: TokenId,
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, Unit] =
    val removePreviousNftBalanceOptionT = for
      nftState <- OptionT:
        PlayNommState[F].token.nftState
          .get(tokenId)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to get nft state of ${tokenId}"
      currentOwner = nftState.currentOwner
      nftBalanceStream <- OptionT.liftF:
        PlayNommState[F].token.nftBalance
          .streamWithPrefix((currentOwner, tokenId).toBytes)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to get nft balance of $currentOwner"
      currentBalance <- OptionT:
        StateT.liftF:
          nftBalanceStream.head.compile.toList
            .leftMap: msg =>
              PlayNommDAppFailure.internal(
                s"Fail to get nft balance of $currentOwner: $msg",
              )
            .map(_.headOption)
      currentUtxoHash = currentBalance._1._3
      _ <- OptionT.liftF:
        for
          _ <- PlayNommState[F].token.nftBalance
            .remove((currentOwner, tokenId, currentUtxoHash))
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Fail to remove nft balance of $currentOwner"
          _ <- StateT.liftF:
            for
              txOption <- TransactionRepository[F]
                .get(currentUtxoHash)
                .leftMap: e =>
                  PlayNommDAppFailure.internal(e.msg)
              tx <- EitherT.fromOption(
                txOption,
                PlayNommDAppFailure.internal(
                  s"Fail to get tx of $currentUtxoHash",
                ),
              )
              _ <- EitherT.right:
                InvalidTxLogger[F].log:
                  InvalidTx(
                    signer = tx.signedTx.sig.account,
                    reason = InvalidReason.CanceledBalance,
                    amountToBurn = BigNat.Zero,
                    tx = tx.signedTx.value,
                    wrongNftInput = Some(tokenId),
                  )
            yield ()
        yield ()
    yield ()

    removePreviousNftBalanceOptionT.value.map(_ => ())
