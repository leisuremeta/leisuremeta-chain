package io.leisuremeta.chain
package bulkinsert

import java.time.temporal.ChronoUnit

import cats.Monad
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
import api.model.token.{NftState, Rarity, TokenDefinitionId, TokenId}
import lib.codec.byte.ByteEncoder.ops.*
import lib.crypto.Hash
import lib.crypto.Hash.ops.*
import lib.datatype.{BigNat, Utf8}
import lib.merkle.MerkleTrieState
import node.dapp.{PlayNommDApp, PlayNommDAppFailure, PlayNommState}
import node.dapp.submodule.*
import node.repository.TransactionRepository

object RecoverTx:
  def apply[F[_]: Async: TransactionRepository: PlayNommState: InvalidTxLogger](
      ms: MerkleTrieState,
      signedTx: Signed.Tx,
  ): EitherT[
    F,
    PlayNommDAppFailure,
    (MerkleTrieState, Option[TransactionWithResult]),
  ] =
    val sig = signedTx.sig
    val tx  = signedTx.value
    tx match
      case ca: Transaction.AccountTx.CreateAccount =>
        val program = for
          accountInfoOption <- PlayNommDAppAccount.getAccountInfo(
            ca.account,
          )
//          _ <- checkExternal(
//            accountInfoOption.isEmpty,
//            s"${ca.account} already exists",
//          )
//          _ <- checkExternal(
//            sig.account == ca.account ||
//              Some(sig.account) == ca.guardian,
//            s"Signer ${sig.account} is neither ${ca.account} nor its guardian",
//          )
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
            externalChainAddresses = ca.ethAddress.fold(Map.empty):
              ethAddress =>
                Map(
                  ExternalChain.ETH -> ExternalChainAddress(ethAddress.utf8),
                )
            ,
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
          accountDataOption <- PlayNommDAppAccount.getAccountInfo(
            ua.account,
          )
          accountData <- fromOption(
            accountDataOption,
            s"${ua.account} does not exists",
          )
//          _ <- checkExternal(
//            sig.account == ua.account ||
//              Some(sig.account) == accountData.guardian,
//            s"Signer ${sig.account} is neither ${ua.account} nor its guardian",
//          )
          accountData1 = accountData.copy(
            guardian = ua.guardian,
            externalChainAddresses = ua.ethAddress.fold(Map.empty):
              ethAddress =>
                Map(
                  ExternalChain.ETH -> ExternalChainAddress(ethAddress.utf8),
                )
            ,
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

//      case ap: Transaction.AccountTx.AddPublicKeySummaries =>
//        val program = for
//          account <- PlayNommState[F].account.name
//            .get(ap.account)
//            .map: account =>
//              scribe.info(s"Account Data: $account")
//              account
//          _ <- StateT.liftF:
//            EitherT.leftT[F, (MerkleTrieState, TransactionWithResult)]:
//              s"Not recovered yet: ${e.msg}"
//        yield TransactionWithResult(signedTx)(None)
//
//        program
//          .run(ms)
//          .map: (ms, txWithResult) =>
//            (ms, Option(txWithResult))
//          .leftMap: msg =>
//            PlayNommDAppFailure.internal(s"Fail to recover error: $msg")

      case tf: Transaction.TokenTx.TransferFungibleToken =>
        val sig = signedTx.sig
//        val tx  = signedTx.value
        val program = for
//          _ <- PlayNommDAppAccount.verifySignature(sig, tx)
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
//              scribe.info(s"DiffBigInt: $diffBigInt")
              EitherT.right:
                InvalidTxLogger[F].log:
                  InvalidTx(
                    signer = sig.account,
                    reason = InvalidReason.OutputMoreThanInput,
                    amountToBurn = BigNat.unsafeFromBigInt(diffBigInt.abs),
                    tx = tf,
                    createdAt = tf.createdAt,
                  )
            else EitherT.pure(())
          txWithResult = TransactionWithResult(Signed(sig, tf))(None)
          txHash       = txWithResult.toHash
          invalidInputs <- removeInputUtxos(
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
//                  scribe.info(s"Sum of Invalid Tx Inputs: $sum")
                  InvalidTxLogger[F].log:
                    InvalidTx(
                      signer = sig.account,
                      reason = InvalidReason.InputAlreadyUsed,
                      amountToBurn = sum,
                      tx = tf,
                      createdAt = tf.createdAt,
                    )
          _ <- tf.outputs.toSeq.traverse:
            case (account, _) =>
              PlayNommDAppToken.putBalance(
                account,
                tf.tokenDefinitionId,
                txHash,
              )
          totalAmount <- fromEitherInternal:
            val either = BigNat.fromBigInt(tokenDef.totalAmount.toBigInt - diffBigInt)
//            if either.isLeft then
//              scribe.info(s"Total Amount: \t${tokenDef.totalAmount}")
//              scribe.info(s"DiffBigInt: \t$diffBigInt")
//              scribe.info(s"Input Amount: \t$inputAmount")
//              scribe.info(s"Output Amount: \t$outputAmount")
//              scribe.info(s"Diff: \t$diffBigInt")
//              scribe.info(s"Either: \t$either")
            either
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

      case mn: Transaction.TokenTx.MintNFT =>
        val program = for
          tokenDefOption <- PlayNommState[F].token.definition
            .get(mn.tokenDefinitionId)
            .mapK:
              PlayNommDAppFailure.mapExternal:
                s"No token definition of ${mn.tokenDefinitionId}"
          nftStateOption <- PlayNommState[F].token.nftState
            .get(mn.tokenId)
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Fail to get nft state of ${mn.tokenId}"
//          _ <- checkExternal(
//            nftStateOption.isEmpty,
//            s"NFT ${mn.tokenId} is already minted",
//          )
          txWithResult = TransactionWithResult(Signed(sig, mn))(None)
          txHash       = txWithResult.toHash
          _ <- PlayNommState[F].token.nftBalance
            .put((mn.output, mn.tokenId, txHash), ())
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Fail to put token balance (${mn.output}, ${mn.tokenId}, $txHash)"
          rarity: Map[Rarity, BigNat] =
            val rarityMapOption: Option[Map[Rarity, BigNat]] = for
              tokenDef <- tokenDefOption
              nftInfo  <- tokenDef.nftInfo
            yield nftInfo.rarity

            rarityMapOption.getOrElse:
              Map(
                Rarity(Utf8.unsafeFrom("LGDY")) -> BigNat
                  .unsafeFromLong(16),
                Rarity(Utf8.unsafeFrom("UNIQ")) -> BigNat
                  .unsafeFromLong(12),
                Rarity(Utf8.unsafeFrom("EPIC")) -> BigNat.unsafeFromLong(8),
                Rarity(Utf8.unsafeFrom("RARE")) -> BigNat.unsafeFromLong(4),
              )
          weight = rarity.getOrElse(mn.rarity, BigNat.unsafeFromLong(2L))
          nftState = NftState(
            tokenId = mn.tokenId,
            tokenDefinitionId = mn.tokenDefinitionId,
            rarity = mn.rarity,
            weight = weight,
            currentOwner = mn.output,
            memo = None,
            lastUpdateTx = txHash,
            previousState = None,
          )
          _ <- PlayNommState[F].token.nftState
            .put(mn.tokenId, nftState)
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Fail to put nft state of ${mn.tokenId}"
          _ <- PlayNommState[F].token.rarityState
            .put((mn.tokenDefinitionId, mn.rarity, mn.tokenId), ())
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Fail to put rarity state of ${mn.tokenDefinitionId}, ${mn.rarity}, ${mn.tokenId}"
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
        val utxoKey      = (sig.account, tn.tokenId, tn.input.toResultHashValue)

        val program = for
          isRemoveSuccessful <- PlayNommState[F].token.nftBalance
            .remove(utxoKey)
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Fail to remove nft balance of $utxoKey"
          _ <-
            if isRemoveSuccessful then StateT.liftF(EitherT.pure(()))
            else if sig.account === Account(Utf8.unsafeFrom("playnomm"))
            then removePreviousNftBalance(tn.tokenId, signedTx)
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
                      createdAt = tn.createdAt,
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

      case bf: Transaction.TokenTx.BurnFungibleToken =>
        val program = for
//          _ <- PlayNommDAppAccount.verifySignature(sig, tx)
          tokenDef <- PlayNommDAppToken.getTokenDefinition(bf.definitionId)
          inputAmount <- PlayNommDAppToken.getFungibleBalanceTotalAmounts(
            bf.inputs.map(_.toResultHashValue),
            sig.account,
          )
          outputAmount <- fromEitherExternal:
            BigNat.tryToSubtract(inputAmount, bf.amount)
          result       = Transaction.TokenTx.BurnFungibleTokenResult(outputAmount)
          txWithResult = TransactionWithResult(Signed(sig, bf))(Some(result))
          txHash       = txWithResult.toHash
          _ <- removeInputUtxos(
            sig.account,
            bf.inputs.map(_.toResultHashValue),
            bf.definitionId,
          )
          _ <- bf.inputs.toList.traverse: inputTxHash =>
            PlayNommDAppToken.removeFungibleSnapshot(
              sig.account,
              bf.definitionId,
              inputTxHash.toResultHashValue,
            )
              .mapK:
                PlayNommDAppFailure.mapInternal:
                  s"Fail to remove fungible snapshot of $inputTxHash"
          _ <-
            if outputAmount === BigNat.Zero then unit
            else
              PlayNommDAppToken.putBalance(sig.account, bf.definitionId, txWithResult.toHash) *>
                PlayNommDAppToken.addFungibleSnapshot(
                  sig.account,
                  bf.definitionId,
                  txWithResult.toHash,
                  outputAmount,
                )
                  .mapK:
                    PlayNommDAppFailure.mapInternal:
                      s"Fail to add fungible snapshot of ${sig.account}"
          totalAmount <- fromEitherInternal:
            BigNat.tryToSubtract(tokenDef.totalAmount, bf.amount)
          _ <- PlayNommDAppToken.putTokenDefinition(
            bf.definitionId,
            tokenDef.copy(totalAmount = totalAmount),
          )
          _ <- PlayNommDAppToken.removeTotalSupplySnapshot(bf.definitionId, bf.amount)
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Fail to remove total supply snapshot of ${bf.definitionId}"
        yield txWithResult

        program
          .run(ms)
          .map: (ms, txWithResult) =>
            (ms, Option(txWithResult))
          .leftMap: msg =>
            PlayNommDAppFailure.internal(s"Fail to recover error: $msg")

      case bn: Transaction.TokenTx.BurnNFT =>
        val program = for
//          _       <- PlayNommDAppAccount.verifySignature(sig, tx)
          tokenId <- getNftTokenId(bn.input.toResultHashValue)
          txWithResult = TransactionWithResult(Signed(sig, bn))(None)
          txHash       = txWithResult.toHash
          utxoKey      = (sig.account, tokenId, bn.input.toResultHashValue)
          isRemoveSuccessful <- PlayNommState[F].token.nftBalance
            .remove(utxoKey)
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Fail to remove nft balance of $utxoKey"
          _ <-
            if isRemoveSuccessful then StateT.liftF(EitherT.pure(()))
            else
              StateT.liftF:
                EitherT.right:
                  InvalidTxLogger[F].log:
                    InvalidTx(
                      signer = sig.account,
                      reason = InvalidReason.BalanceNotExist,
                      amountToBurn = BigNat.Zero,
                      tx = bn,
                      wrongNftInput = Some(tokenId),
                      createdAt = bn.createdAt,
                    )
          nftStateOption <- PlayNommState[F].token.nftState
            .get(tokenId)
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Fail to get nft state of ${tokenId}"
          _ <- nftStateOption.traverse: nftState =>
            for
              _ <- PlayNommState[F].token.nftState
                .remove(tokenId)
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to remove nft state of ${tokenId}"
              _ <- PlayNommState[F].token.rarityState
                .remove((bn.definitionId, nftState.rarity, tokenId))
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to remove rarity state of ${tokenId}"
            yield ()
        yield txWithResult

        program
          .run(ms)
          .map: (ms, txWithResult) =>
            (ms, Option(txWithResult))
          .leftMap: msg =>
            PlayNommDAppFailure.internal(s"Fail to recover error: $msg")

      case ef: Transaction.TokenTx.EntrustFungibleToken =>
        val sig = signedTx.sig
//        val tx  = signedTx.value
        val program = for
//          _        <- PlayNommDAppAccount.verifySignature(sig, tx)
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
                      amountToBurn = BigNat.unsafeFromBigInt(diffBigInt.abs),
                      tx = ef,
                      createdAt = ef.createdAt,
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
          invalidInputs <- removeInputUtxos(
            sig.account,
            ef.inputs.map(_.toResultHashValue),
            ef.definitionId,
          )
          _ <- StateT.liftF:
            if invalidInputs.isEmpty then EitherT.pure(())
            else
              invalidInputs
                .traverse(TransactionRepository[F].get)
                .leftMap: e =>
                  PlayNommDAppFailure.internal(s"Fail to get tx: $e")
                .semiflatMap: txOptions =>
                  val sum = txOptions
                    .map: txOption =>
                      txOption.fold(BigNat.Zero):
                        PlayNommDAppToken.tokenBalanceAmount(sig.account)
                    .foldLeft(BigNat.Zero)(BigNat.add)
                  InvalidTxLogger[F].log:
                    InvalidTx(
                      signer = sig.account,
                      reason = InvalidReason.InputAlreadyUsed,
                      amountToBurn = sum,
                      tx = ef,
                      createdAt = ef.createdAt,
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
        val txWithResult = TransactionWithResult(Signed(sig, ef))(None)
        val txHash       = txWithResult.toHash
        val program = for
//          _ <- PlayNommDAppAccount.verifySignature(sig, tx)
          inputTxHashes <- PlayNommState[F].token.nftBalance
            .streamWithPrefix((sig.account, ef.tokenId).toBytes)
            .flatMapF: stream =>
              stream.map(_._1._3).compile.toList
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Fail to get nft balance stream of (${sig.account}, ${ef.tokenId})"
          txWithResultOption <- inputTxHashes.headOption match
            case None =>
//              scribe.info(s"No input tx hash found for ${sig.account} and ${ef.tokenId}")
              StateT.liftF:
                EitherT.right:
                  InvalidTxLogger[F]
                    .log:
                      InvalidTx(
                        signer = sig.account,
                        reason = InvalidReason.BalanceNotExist,
                        amountToBurn = BigNat.Zero,
                        tx = ef,
                        wrongNftInput = Some(ef.tokenId),
                        createdAt = ef.createdAt,
                      )
                    .map(_ => None)
            case Some(inputTxHash) =>
//              scribe.info(s"Input tx hash found for ${sig.account} and ${ef.tokenId}: $inputTxHash")
              val utxoKey = (sig.account, ef.tokenId, inputTxHash)
              for
                isRemoveSuccessful <- PlayNommState[F].token.nftBalance
                  .remove(utxoKey)
                  .mapK:
                    PlayNommDAppFailure.mapInternal:
                      s"Fail to remove nft balance of $utxoKey"
                _ <- StateT.liftF:
//                  scribe.info(s"Is remove successful: $isRemoveSuccessful")
                  if !isRemoveSuccessful then
                    EitherT.right:
                      InvalidTxLogger[F].log:
                        InvalidTx(
                          signer = sig.account,
                          reason = InvalidReason.BalanceNotExist,
                          amountToBurn = BigNat.Zero,
                          tx = ef,
                          wrongNftInput = Some(ef.tokenId),
                          createdAt = ef.createdAt,
                        )
                  else EitherT.pure(())
                newUtxoKey = (sig.account, ef.to, ef.tokenId, txHash)
                _ <- PlayNommState[F].token.entrustNftBalance
                  .put(newUtxoKey, ())
                  .mapK:
                    PlayNommDAppFailure.mapInternal:
                      s"Fail to put entrust nft balance of $newUtxoKey"
              yield
//                scribe.info(s"Succeed to recover EntrustNFT: $txWithResult")
                Some(txWithResult)
        yield txWithResultOption

        program
          .run(ms)
          .leftMap: msg =>
            PlayNommDAppFailure.internal(s"Fail to recover error: $msg")

      case de: Transaction.TokenTx.DisposeEntrustedFungibleToken =>
        val program = for
//          _        <- PlayNommDAppAccount.verifySignature(sig, tx)
          tokenDef <- PlayNommDAppToken.getTokenDefinition(de.definitionId)
          inputHashList = de.inputs.map(_.toResultHashValue)
          inputMap <- PlayNommDAppToken.getEntrustedInputs(inputHashList, sig.account)
          inputAmount  = inputMap.values.foldLeft(BigNat.Zero)(BigNat.add)
          outputAmount = de.outputs.values.foldLeft(BigNat.Zero)(BigNat.add)
//          _ <- checkExternal(
//            inputAmount === outputAmount,
//            s"Output amount is not equal to input amount $inputAmount",
//          )
          txWithResult = TransactionWithResult(Signed(sig, de))(None)
          txHash       = txWithResult.toHash
          _ <- inputMap.toList
            .map(_._1)
            .traverse: (account, txHash) =>
              PlayNommState[F].token.entrustFungibleBalance
                .remove((account, sig.account, de.definitionId, txHash))
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to remove entrust fungible balance of (${account}, ${sig.account}, ${de.definitionId}, ${txHash})"
                *> PlayNommDAppToken.removeFungibleSnapshot[F](account, de.definitionId, txHash)
                  .mapK:
                    PlayNommDAppFailure.mapInternal:
                      s"Fail to remove fungible snapshot of $txHash"
          _ <- de.outputs.toList.traverse: (account, amount) =>
            PlayNommState[F].token.fungibleBalance
              .put((account, de.definitionId, txHash), ())
              .mapK:
                PlayNommDAppFailure.mapInternal:
                  s"Fail to put fungible balance of (${account}, ${de.definitionId}, ${txHash})"
              *> PlayNommDAppToken.addFungibleSnapshot[F](account, de.definitionId, txHash, amount)
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to put fungible snapshot of $txHash"
        yield txWithResult

        program
          .run(ms)
          .map: (ms, txWithResult) =>
            (ms, Option(txWithResult))
          .leftMap: msg =>
            PlayNommDAppFailure.internal(s"Fail to recover error: $msg")

      case de: Transaction.TokenTx.DisposeEntrustedNFT =>
        val txWithResult = TransactionWithResult(Signed(sig, de))(None)
        val txHash       = txWithResult.toHash
        val program = for
          inputOption <- StateT.liftF:
            TransactionRepository[F]
              .get(de.input.toResultHashValue)
              .leftMap: e =>
                PlayNommDAppFailure.internal:
                  s"Fail to get tx ${de.input}: ${e.msg}"
              .map: txOption =>
                txOption.flatMap: txWithResult =>
                  txWithResult.signedTx.value match
                    case ef: Transaction.TokenTx.EntrustNFT
                        if ef.to === sig.account =>
                      Some(
                        (
                          txWithResult,
                          ef.tokenId,
                          txWithResult.signedTx.sig.account,
                        ),
                      )
                    case _ =>
                      None
          txWithResultOption <- inputOption match
            case Some((inputTx, tokenId, from)) =>
              val utxoKey = (
                from,
                sig.account,
                de.tokenId,
                de.input.toResultHashValue,
              )
              for
                isRemoveSuccessful <- PlayNommState[
                  F,
                ].token.entrustNftBalance
                  .remove(utxoKey)
                  .mapK:
                    PlayNommDAppFailure.mapInternal:
                      s"Fail to remove entrust nft balance of $utxoKey"
                _ <-
                  if isRemoveSuccessful then unit[F]
                  else
                    removePreviousNftBalance[F](de.tokenId, signedTx) *> StateT.liftF:
                      EitherT.right:
                        InvalidTxLogger[F].log:
                          InvalidTx(
                            signer = sig.account,
                            reason = InvalidReason.BalanceNotExist,
                            amountToBurn = BigNat.Zero,
                            tx = de,
                            wrongNftInput = Some(de.tokenId),
                            createdAt = de.createdAt,
                          )
                toAccount  = de.output.getOrElse(from)
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
            case None =>
              for
                to <- de.output.traverse: output =>
                  PlayNommState[F].token.nftBalance
                    .streamWithPrefix(output.toBytes)
                    .mapK:
                      PlayNommDAppFailure.mapInternal:
                        s"Fail to get nft balance of ${output}"
                    .flatMapF: stream =>
                      stream
                        .filter:
                          case ((account, tokenId, txHash), _) =>
                            tokenId == de.tokenId
                        .head
                        .compile
                        .toList
                        .map: list =>
                          list.headOption.map(_._1._1)
                        .leftMap: msg =>
                          PlayNommDAppFailure.internal:
                            s"Fail to get nft balance stream: $msg"
                txWithResultOption <- to.flatten match
                  case Some(account) =>
                    unit[F].map: _ =>
                      None
                  case None =>
                    for
                      stateOption <- PlayNommState[F].token.nftState
                        .get(de.tokenId)
                        .mapK:
                          PlayNommDAppFailure.mapInternal:
                            s"Fail to get nft state of ${de.tokenId}"
                      currentOwnerOption = stateOption.map(_.currentOwner)
                    yield Some(txWithResult)
              yield txWithResultOption
        yield txWithResultOption

        program
          .run(ms)
          .leftMap: msg =>
            PlayNommDAppFailure.internal(s"Fail to recover error: $msg")

      case tf: Transaction.RewardTx.OfferReward =>
        val sig = signedTx.sig
//        val tx  = signedTx.value
        val program = for
//          _ <- PlayNommDAppAccount.verifySignature(sig, tx)
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
                    createdAt = tf.createdAt,
                  )
            else EitherT.pure(())
          txWithResult = TransactionWithResult(Signed(sig, tf))(None)
          txHash       = txWithResult.toHash
          invalidInputs <- removeInputUtxos(
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
                      createdAt = tf.createdAt,
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

      case ss: Transaction.AgendaTx.SuggestSimpleAgenda =>
        EitherT.pure((ms, Some(TransactionWithResult(Signed(sig, ss))(None))))

      case vs: Transaction.AgendaTx.VoteSimpleAgenda =>
        EitherT.pure((ms, Some(TransactionWithResult(Signed(sig, vs))(None))))

      case _ =>
        PlayNommDApp[F](signedTx)
          .run(ms)
          .map: (ms, txWithResult) =>
            (ms, Option(txWithResult))

  def removePreviousNftBalance[F[_]
    : Async: PlayNommState: TransactionRepository: InvalidTxLogger](
      tokenId: TokenId,
      signedTx: Signed.Tx,
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
            EitherT.right:
              InvalidTxLogger[F].log:
                InvalidTx(
                  signer = signedTx.sig.account,
                  reason = InvalidReason.CanceledBalance,
                  amountToBurn = BigNat.Zero,
                  tx = signedTx.value,
                  wrongNftInput = Some(tokenId),
                  createdAt = signedTx.value.createdAt,
                )
        yield ()
    yield ()

    removePreviousNftBalanceOptionT.value.map(_ => ())

  def getNftTokenId[F[_]: Monad: TransactionRepository](
      utxoHash: Hash.Value[TransactionWithResult],
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, TokenId] =
    StateT.liftF:
      TransactionRepository[F]
        .get(utxoHash)
        .leftMap(e => PlayNommDAppFailure.internal(s"Fail to get tx: ${e.msg}"))
        .subflatMap: txOption =>
          Either.fromOption(
            txOption,
            PlayNommDAppFailure.internal(s"There is no tx of $utxoHash"),
          )
        .flatMap: txWithResult =>
          txWithResult.signedTx.value match
            case nb: Transaction.NftBalance =>
              EitherT.pure:
                nb match
                  case mn: Transaction.TokenTx.MintNFT     => mn.tokenId
                  case tn: Transaction.TokenTx.TransferNFT => tn.tokenId
                  case den: Transaction.TokenTx.DisposeEntrustedNFT =>
                    den.tokenId
                  case mnm: Transaction.TokenTx.MintNFTWithMemo => mnm.tokenId
            case _ =>
              EitherT.leftT:
                PlayNommDAppFailure.external:
                  s"Tx $txWithResult is not a nft balance"

  def removeInputUtxos[F[_]: Monad: PlayNommState](
      account: Account,
      inputs: Set[Hash.Value[TransactionWithResult]],
      definitionId: TokenDefinitionId,
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, List[
    Hash.Value[TransactionWithResult],
  ]] =
    val inputList = inputs.toList
    for
      removeResults <- inputList.traverse: txHash =>
        PlayNommState[F].token.fungibleBalance
          .remove((account, definitionId, txHash))
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to remove fingible balance ($account, $definitionId, $txHash)"
      invalidUtxos = inputList.zip(removeResults).filterNot(_._2).map(_._1)
    yield invalidUtxos
