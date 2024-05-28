package io.leisuremeta.chain
package node
package dapp
package submodule

import cats.Monad
import cats.data.{EitherT, StateT}
import cats.effect.Concurrent
import cats.syntax.all.*

import api.model.{
  Account,
  AccountSignature,
  Signed,
  Transaction,
  TransactionWithResult,
}
import api.model.TransactionWithResult.ops.*
import api.model.token.*
import api.model.token.SnapshotState.SnapshotId.*
import lib.crypto.Hash
import lib.crypto.Hash.ops.*
import lib.datatype.BigNat
import lib.merkle.MerkleTrieState
import repository.TransactionRepository

object PlayNommDAppToken:
  def apply[F[_]: Concurrent: PlayNommState: TransactionRepository](
      tx: Transaction.TokenTx,
      sig: AccountSignature,
  ): StateT[
    EitherT[F, PlayNommDAppFailure, *],
    MerkleTrieState,
    TransactionWithResult,
  ] = tx match
    case dt: Transaction.TokenTx.DefineToken =>
      for
        _              <- PlayNommDAppAccount.verifySignature(sig, tx)
        tokenDefOption <- getTokenDefinitionOption(dt.definitionId)
        _ <- checkExternal(
          tokenDefOption.isEmpty,
          s"Token ${dt.definitionId} is already defined",
        )
        tokenDefinition = TokenDefinition(
          id = dt.definitionId,
          name = dt.name,
          symbol = dt.symbol,
          adminGroup = dt.minterGroup,
          totalAmount = BigNat.Zero,
          nftInfo = dt.nftInfo.map(NftInfoWithPrecision.fromNftInfo),
        )
        _ <- putTokenDefinition(dt.definitionId, tokenDefinition)
      yield TransactionWithResult(Signed(sig, dt))(None)

    case dp: Transaction.TokenTx.DefineTokenWithPrecision =>
      for
        _              <- PlayNommDAppAccount.verifySignature(sig, tx)
        tokenDefOption <- getTokenDefinitionOption(dp.definitionId)
        _ <- checkExternal(
          tokenDefOption.isEmpty,
          s"Token ${dp.definitionId} is already defined",
        )
        tokenDefinition = TokenDefinition(
          id = dp.definitionId,
          name = dp.name,
          symbol = dp.symbol,
          adminGroup = dp.minterGroup,
          totalAmount = BigNat.Zero,
          nftInfo = dp.nftInfo,
        )
        _ <- putTokenDefinition(dp.definitionId, tokenDefinition)
      yield TransactionWithResult(Signed(sig, dp))(None)

    case mf: Transaction.TokenTx.MintFungibleToken =>
      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        tokenDef <- checkMinterAndGetTokenDefinition(
          sig.account,
          mf.definitionId,
        )
        txWithResult = TransactionWithResult(Signed(sig, mf))(None)
        txHash       = txWithResult.toHash
        _ <- mf.outputs.toSeq.traverse { case (account, _) =>
          PlayNommState[F].token.fungibleBalance
            .put((account, mf.definitionId, txHash), ())
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Fail to put token balance ($account, ${mf.definitionId}, $txHash)"
        }
        mintAmount  = mf.outputs.values.foldLeft(BigNat.Zero)(BigNat.add)
        totalAmount = BigNat.add(tokenDef.totalAmount, mintAmount)
        _ <- putTokenDefinition(
          mf.definitionId,
          tokenDef.copy(totalAmount = totalAmount),
        )
      yield txWithResult

    case mn: Transaction.TokenTx.MintNFT =>
      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        tokenDef <- checkMinterAndGetTokenDefinition(
          sig.account,
          mn.tokenDefinitionId,
        )
        nftStateOption <- PlayNommState[F].token.nftState
          .get(mn.tokenId)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to get nft state of ${mn.tokenId}"
        _ <- checkExternal(
          nftStateOption.isEmpty,
          s"NFT ${mn.tokenId} is already minted",
        )
        txWithResult = TransactionWithResult(Signed(sig, mn))(None)
        txHash       = txWithResult.toHash
        _ <- PlayNommState[F].token.nftBalance
          .put((mn.output, mn.tokenId, txHash), ())
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to put token balance (${mn.output}, ${mn.tokenId}, $txHash)"
        weight = tokenDef.nftInfo.get.rarity
          .getOrElse(mn.rarity, BigNat.unsafeFromLong(2L))
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
        _ <- PlayNommState[F].token.nftHistory
          .put(txHash, nftState)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to put nft history of ${mn.tokenId} of $txHash"
        _ <- PlayNommState[F].token.rarityState
          .put((mn.tokenDefinitionId, mn.rarity, mn.tokenId), ())
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to put rarity state of ${mn.tokenDefinitionId}, ${mn.rarity}, ${mn.tokenId}"
      yield txWithResult

    case mn: Transaction.TokenTx.MintNFTWithMemo =>
      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        tokenDef <- checkMinterAndGetTokenDefinition(
          sig.account,
          mn.tokenDefinitionId,
        )
        nftStateOption <- PlayNommState[F].token.nftState
          .get(mn.tokenId)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to get nft state of ${mn.tokenId}"
        _ <- checkExternal(
          nftStateOption.isEmpty,
          s"NFT ${mn.tokenId} is already minted",
        )
        txWithResult = TransactionWithResult(Signed(sig, mn))(None)
        txHash       = txWithResult.toHash
        _ <- PlayNommState[F].token.nftBalance
          .put((mn.output, mn.tokenId, txHash), ())
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to put token balance (${mn.output}, ${mn.tokenId}, $txHash)"
        weight = tokenDef.nftInfo.get.rarity
          .getOrElse(mn.rarity, BigNat.unsafeFromLong(2L))
        nftState = NftState(
          tokenId = mn.tokenId,
          tokenDefinitionId = mn.tokenDefinitionId,
          rarity = mn.rarity,
          weight = weight,
          currentOwner = mn.output,
          memo = mn.memo,
          lastUpdateTx = txHash,
          previousState = None,
        )
        _ <- PlayNommState[F].token.nftState
          .put(mn.tokenId, nftState)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to put nft state of ${mn.tokenId}"
        _ <- PlayNommState[F].token.nftHistory
          .put(txHash, nftState)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to put nft history of ${mn.tokenId} of $txHash"
        _ <- PlayNommState[F].token.rarityState
          .put((mn.tokenDefinitionId, mn.rarity, mn.tokenId), ())
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to put rarity state of ${mn.tokenDefinitionId}, ${mn.rarity}, ${mn.tokenId}"
      yield txWithResult

    case un: Transaction.TokenTx.UpdateNFT =>
      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        tokenDef <- checkMinterAndGetTokenDefinition(
          sig.account,
          un.tokenDefinitionId,
        )
        nftStateOption <- PlayNommState[F].token.nftState
          .get(un.tokenId)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to get nft state of ${un.tokenId}"
        nftState <- fromOption(
          nftStateOption,
          s"Empty NFT State: ${un.tokenId}",
        )
        txWithResult = TransactionWithResult(Signed(sig, un))(None)
        txHash       = txWithResult.toHash
        weight = tokenDef.nftInfo.get.rarity
          .getOrElse(un.rarity, BigNat.unsafeFromLong(2L))
        nftState1 = NftState(
          tokenId = un.tokenId,
          tokenDefinitionId = un.tokenDefinitionId,
          rarity = un.rarity,
          weight = weight,
          currentOwner = un.output,
          memo = un.memo,
          lastUpdateTx = txHash,
          previousState = Some(nftState.lastUpdateTx),
        )
        _ <- PlayNommState[F].token.nftState
          .put(un.tokenId, nftState1)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to put nft state of ${un.tokenId}"
        _ <- PlayNommState[F].token.nftHistory
          .put(txHash, nftState1)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to put nft history of ${un.tokenId} of $txHash"
      yield txWithResult

    case tf: Transaction.TokenTx.TransferFungibleToken =>
      for
        _        <- PlayNommDAppAccount.verifySignature(sig, tx)
        tokenDef <- getTokenDefinition(tf.tokenDefinitionId)
        inputAmount <- getFungibleBalanceTotalAmounts(
          tf.inputs.map(_.toResultHashValue),
          sig.account,
        )
        outputAmount = tf.outputs.values.foldLeft(BigNat.Zero)(BigNat.add)
        diff <- fromEitherExternal:
          BigNat.tryToSubtract(inputAmount, outputAmount)
        txWithResult = TransactionWithResult(Signed(sig, tf))(None)
        txHash       = txWithResult.toHash
        _ <- removeInputUtxos(
          sig.account,
          tf.inputs.map(_.toResultHashValue),
          tf.tokenDefinitionId,
        )
        _ <- tf.outputs.toSeq.traverse:
          case (account, _) =>
            putBalance(account, tf.tokenDefinitionId, txHash)
        totalAmount <- fromEitherInternal:
          BigNat.tryToSubtract(tokenDef.totalAmount, diff)
        _ <- putTokenDefinition(
          tf.tokenDefinitionId,
          tokenDef.copy(totalAmount = totalAmount),
        )
      yield txWithResult

    case tn: Transaction.TokenTx.TransferNFT =>
      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        txWithResult = TransactionWithResult(Signed(sig, tn))(None)
        txHash       = txWithResult.toHash
        utxoKey      = (sig.account, tn.tokenId, tn.input.toResultHashValue)
        isRemoveSuccessful <- PlayNommState[F].token.nftBalance
          .remove(utxoKey)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to remove nft balance of $utxoKey"
        _ <- checkExternal(isRemoveSuccessful, s"No NFT Balance: ${utxoKey}")
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

    case bf: Transaction.TokenTx.BurnFungibleToken =>
      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        tokenDef <- checkMinterAndGetTokenDefinition(
          sig.account,
          bf.definitionId,
        )
        inputAmount <- getFungibleBalanceTotalAmounts(
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
        _ <-
          if outputAmount === BigNat.Zero then unit
          else putBalance(sig.account, bf.definitionId, txWithResult.toHash)
        totalAmount <- fromEitherInternal:
          BigNat.tryToSubtract(tokenDef.totalAmount, bf.amount)
        _ <- putTokenDefinition(
          bf.definitionId,
          tokenDef.copy(totalAmount = totalAmount),
        )
      yield txWithResult

    case bn: Transaction.TokenTx.BurnNFT =>
      for
        _       <- PlayNommDAppAccount.verifySignature(sig, tx)
        tokenId <- getNftTokenId(bn.input.toResultHashValue)
        txWithResult = TransactionWithResult(Signed(sig, bn))(None)
        txHash       = txWithResult.toHash
        utxoKey      = (sig.account, tokenId, bn.input.toResultHashValue)
        isRemoveSuccessful <- PlayNommState[F].token.nftBalance
          .remove(utxoKey)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to remove nft balance of $utxoKey"
        _ <- checkExternal(isRemoveSuccessful, s"No NFT Balance: ${utxoKey}")
        nftStateOption <- PlayNommState[F].token.nftState
          .get(tokenId)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to get nft state of ${tokenId}"
        _ <- checkInternal(
          nftStateOption.isDefined,
          s"Empty NFT State: ${tokenId}",
        )
        nftState <- fromOption(
          nftStateOption,
          s"Empty NFT State: ${tokenId}",
        )
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
      yield txWithResult

    case ef: Transaction.TokenTx.EntrustFungibleToken =>
      for
        _        <- PlayNommDAppAccount.verifySignature(sig, tx)
        tokenDef <- getTokenDefinition(ef.definitionId)
        inputAmount <- getFungibleBalanceTotalAmounts(
          ef.inputs.map(_.toResultHashValue),
          sig.account,
        )
        diff <- fromEitherExternal:
          BigNat.tryToSubtract(inputAmount, ef.amount)
        result       = Transaction.TokenTx.EntrustFungibleTokenResult(diff)
        txWithResult = TransactionWithResult(Signed(sig, ef))(Some(result))
        txHash       = txWithResult.toHash
        _ <- removeInputUtxos(
          sig.account,
          ef.inputs.map(_.toResultHashValue),
          ef.definitionId,
        )
        _ <- PlayNommState[F].token.entrustFungibleBalance
          .put((sig.account, ef.to, ef.definitionId, txHash), ())
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to put entrust fungible balance of (${sig.account}, ${ef.to}, ${ef.definitionId}, ${txHash})"
        _ <- putBalance(sig.account, ef.definitionId, txHash)
      yield txWithResult

    case de: Transaction.TokenTx.DisposeEntrustedFungibleToken =>
      for
        _        <- PlayNommDAppAccount.verifySignature(sig, tx)
        tokenDef <- getTokenDefinition(de.definitionId)
        inputHashList = de.inputs.map(_.toResultHashValue)
        inputMap <- getEntrustedInputs(inputHashList, sig.account)
        inputAmount  = inputMap.values.foldLeft(BigNat.Zero)(BigNat.add)
        outputAmount = de.outputs.values.foldLeft(BigNat.Zero)(BigNat.add)
        _ <- checkExternal(
          inputAmount === outputAmount,
          s"Output amount is not equal to input amount $inputAmount",
        )
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
        _ <- de.outputs.toList.traverse: (account, _) =>
          PlayNommState[F].token.fungibleBalance
            .put((account, de.definitionId, txHash), ())
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Fail to put fungible balance of (${account}, ${de.definitionId}, ${txHash})"
      yield txWithResult

    case ef: Transaction.TokenTx.EntrustNFT =>
      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        txWithResult = TransactionWithResult(Signed(sig, ef))(None)
        txHash       = txWithResult.toHash
        utxoKey      = (sig.account, ef.tokenId, ef.input.toResultHashValue)
        isRemoveSuccessful <- PlayNommState[F].token.nftBalance
          .remove(utxoKey)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to remove nft balance of $utxoKey"
        _ <- checkExternal(isRemoveSuccessful, s"No NFT Balance: ${utxoKey}")
        newUtxoKey = (sig.account, ef.to, ef.tokenId, txHash)
        _ <- PlayNommState[F].token.entrustNftBalance
          .put(newUtxoKey, ())
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to put entrust nft balance of $newUtxoKey"
      yield txWithResult

    case de: Transaction.TokenTx.DisposeEntrustedNFT =>
      for
        _            <- PlayNommDAppAccount.verifySignature(sig, tx)
        entrustedNFT <- getEntrustedNFT(de.input.toResultHashValue, sig.account)
        (fromAccount, entrustTx) = entrustedNFT
        txWithResult             = TransactionWithResult(Signed(sig, de))(None)
        txHash                   = txWithResult.toHash
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
        _ <- checkExternal(
          isRemoveSuccessful,
          s"No Entrust NFT Balance: ${utxoKey}",
        )
        newUtxoKey = (de.output.getOrElse(fromAccount), de.tokenId, txHash)
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
      yield txWithResult

    case cs: Transaction.TokenTx.CreateSnapshot =>
      for
        _              <- PlayNommDAppAccount.verifySignature(sig, tx)
        tokenDefOption <- getTokenDefinitionOption(cs.definitionId)
        tokenDef <- fromOption(
          tokenDefOption,
          s"Token ${cs.definitionId} is not defined",
        )
        adminGroupId <- fromOption(
          tokenDef.adminGroup,
          s"No admin group in token ${cs.definitionId}",
        )
        _ <- PlayNommState[F].group.groupAccount
          .get((adminGroupId, sig.account))
          .mapK:
            PlayNommDAppFailure.mapExternal:
              s"Not in admin group ${adminGroupId} of ${sig.account}"
        snapshotStateOption <- PlayNommState[F].token.snapshotState
          .get(cs.definitionId)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to get snapshot state of ${cs.definitionId}"
        txWithResult = TransactionWithResult(Signed(sig, cs))(None)
        snapshotId = snapshotStateOption.fold(SnapshotState.SnapshotId.Zero):
          _.snapshotId.increase
        snapshotState = SnapshotState(
          snapshotId = snapshotId,
          createdAt = cs.createdAt,
          txHash = txWithResult.toHash.toSignedTxHash,
          memo = cs.memo,
        )
        _ <- PlayNommState[F].token.snapshotState
          .put(cs.definitionId, snapshotState)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to put snapshot state of ${cs.definitionId}"
      yield txWithResult

  def getTokenDefinitionOption[F[_]: Monad: PlayNommState](
      definitionId: TokenDefinitionId,
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, Option[
    TokenDefinition,
  ]] =
    PlayNommState[F].token.definition
      .get(definitionId)
      .mapK:
        PlayNommDAppFailure.mapInternal:
          s"Fail to get token definition of ${definitionId}"

  def getTokenDefinition[F[_]: Monad: PlayNommState](
      definitionId: TokenDefinitionId,
  ): StateT[
    EitherT[F, PlayNommDAppFailure, *],
    MerkleTrieState,
    TokenDefinition,
  ] =
    for
      tokenDefOption <- getTokenDefinitionOption(definitionId)
      tokenDef <- fromOption(
        tokenDefOption,
        s"Token $definitionId is not defined",
      )
    yield tokenDef

  def putTokenDefinition[F[_]: Monad: PlayNommState](
      definitionId: TokenDefinitionId,
      definition: TokenDefinition,
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, Unit] =
    PlayNommState[F].token.definition
      .put(definitionId, definition)
      .mapK(PlayNommDAppFailure.mapInternal {
        s"Fail to set token definition of $definitionId"
      })

  def checkMinterAndGetTokenDefinition[F[_]: Monad: PlayNommState](
      account: Account,
      definitionId: TokenDefinitionId,
  ): StateT[
    EitherT[F, PlayNommDAppFailure, *],
    MerkleTrieState,
    TokenDefinition,
  ] =
    for
      tokenDef <- getTokenDefinition(definitionId)
      minterGroup <- fromOption(
        tokenDef.adminGroup,
        s"Token $definitionId does not have a minter group",
      )
      groupAccountInfoOption <- PlayNommState[F].group.groupAccount
        .get((minterGroup, account))
        .mapK:
          PlayNommDAppFailure.mapInternal:
            s"Fail to get group account for ($minterGroup, $account)"
      _ <- checkExternal(
        groupAccountInfoOption.nonEmpty,
        s"Account $account is not a member of minter group $minterGroup",
      )
    yield tokenDef

  def getFungibleBalanceTotalAmounts[F[_]
    : Monad: TransactionRepository: PlayNommState](
      inputs: Set[Hash.Value[TransactionWithResult]],
      account: Account,
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, BigNat] =
    StateT.liftF:
      inputs.toSeq
        .traverse: txHash =>
          for
            txOption <- TransactionRepository[F]
              .get(txHash)
              .leftMap: e =>
                PlayNommDAppFailure.internal(
                  s"Fail to get tx $txHash: ${e.msg}",
                )
            txWithResult <- EitherT.fromOption(
              txOption,
              PlayNommDAppFailure.internal(s"There is no tx of $txHash"),
            )
          yield tokenBalanceAmount(account)(txWithResult)
        .map { _.foldLeft(BigNat.Zero)(BigNat.add) }

  def removeInputUtxos[F[_]: Monad: PlayNommState](
      account: Account,
      inputs: Set[Hash.Value[TransactionWithResult]],
      definitionId: TokenDefinitionId,
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, List[
    Hash.Value[TransactionWithResult],
  ]] =
    val inputList = inputs.toList
    for
      removeResults <- inputList.traverse { txHash =>
        PlayNommState[F].token.fungibleBalance
          .remove((account, definitionId, txHash))
          .mapK(PlayNommDAppFailure.mapInternal {
            s"Fail to remove fingible balance ($account, $definitionId, $txHash)"
          })
      }
      invalidUtxos = inputList.zip(removeResults).filterNot(_._2).map(_._1)
      _ <- checkExternal(
        invalidUtxos.isEmpty,
        s"These utxos are invalid: $invalidUtxos",
      )
    yield invalidUtxos

  def tokenBalanceAmount(account: Account)(
      txWithResult: TransactionWithResult,
  ): BigNat = txWithResult.signedTx.value match
    case tb: Transaction.FungibleBalance =>
      tb match
        case mt: Transaction.TokenTx.MintFungibleToken =>
          mt.outputs.getOrElse(account, BigNat.Zero)
        case bt: Transaction.TokenTx.BurnFungibleToken =>
          txWithResult.result.fold(BigNat.Zero):
            case Transaction.TokenTx.BurnFungibleTokenResult(amount)
                if txWithResult.signedTx.sig.account === account =>
              amount
            case _ =>
              scribe.error(
                s"Fail to get burn token result: $txWithResult",
              )
              BigNat.Zero
        case tt: Transaction.TokenTx.TransferFungibleToken =>
          tt.outputs.getOrElse(account, BigNat.Zero)
        case ef: Transaction.TokenTx.EntrustFungibleToken =>
          txWithResult.result.fold(BigNat.Zero):
            case Transaction.TokenTx.EntrustFungibleTokenResult(remainder) =>
              remainder
            case _ => BigNat.Zero
        case df: Transaction.TokenTx.DisposeEntrustedFungibleToken =>
          df.outputs
            .get(txWithResult.signedTx.sig.account)
            .getOrElse(BigNat.Zero)
        case or: Transaction.RewardTx.OfferReward =>
          or.outputs
            .get(txWithResult.signedTx.sig.account)
            .getOrElse(BigNat.Zero)
        case xr: Transaction.RewardTx.ExecuteReward =>
          txWithResult.result.fold(BigNat.Zero):
            case Transaction.RewardTx.ExecuteRewardResult(outputs) =>
              outputs
                .get(txWithResult.signedTx.sig.account)
                .getOrElse(BigNat.Zero)
            case _ => BigNat.Zero
        case xo: Transaction.RewardTx.ExecuteOwnershipReward =>
          txWithResult.result.fold(BigNat.Zero):
            case Transaction.RewardTx.ExecuteOwnershipRewardResult(outputs) =>
              outputs
                .get(txWithResult.signedTx.sig.account)
                .getOrElse(BigNat.Zero)
            case _ => BigNat.Zero
    case _ =>
      scribe.error(s"Not a fungible balance: $txWithResult")
      BigNat.Zero

  def putBalance[F[_]: Monad: PlayNommState](
      account: Account,
      definitionId: TokenDefinitionId,
      txHash: Hash.Value[TransactionWithResult],
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, Unit] =
    PlayNommState[F].token.fungibleBalance
      .put((account, definitionId, txHash), ())
      .mapK:
        PlayNommDAppFailure.mapInternal:
          s"Fail to put token balance ($account, $definitionId, $txHash)"

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
                  case mn: Transaction.TokenTx.MintNFT          => mn.tokenId
                  case mnm: Transaction.TokenTx.MintNFTWithMemo => mnm.tokenId
                  case tn: Transaction.TokenTx.TransferNFT      => tn.tokenId
                  case den: Transaction.TokenTx.DisposeEntrustedNFT =>
                    den.tokenId
            case _ =>
              EitherT.leftT:
                PlayNommDAppFailure.external:
                  s"Tx $txWithResult is not a nft balance"

  def getEntrustedInputs[F[_]: Monad: TransactionRepository](
      inputs: Set[Hash.Value[TransactionWithResult]],
      account: Account,
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, Map[
    (Account, Hash.Value[TransactionWithResult]),
    BigNat,
  ]] =
    StateT.liftF:
      inputs.toSeq
        .traverse: txHash =>
          for
            txOption <- TransactionRepository[F]
              .get(txHash)
              .leftMap: e =>
                PlayNommDAppFailure.internal:
                  s"Fail to get tx $txHash: ${e.msg}"
            txWithResult <- EitherT.fromOption(
              txOption,
              PlayNommDAppFailure.internal(s"There is no tx of $txHash"),
            )
            amount <- txWithResult.signedTx.value match
              case ef: Transaction.TokenTx.EntrustFungibleToken =>
                EitherT.cond(
                  ef.to === account,
                  ef.amount,
                  PlayNommDAppFailure.external(
                    s"Entrust fungible token tx $txWithResult is not for $account",
                  ),
                )
              case _ =>
                EitherT.leftT:
                  PlayNommDAppFailure.external(
                    s"Tx $txWithResult is not an entrust fungible token transaction",
                  )
          yield ((txWithResult.signedTx.sig.account, txHash), amount)
        .map(_.toMap)

  def getEntrustedNFT[F[_]: Monad: TransactionRepository](
      input: Hash.Value[TransactionWithResult],
      account: Account,
  ): StateT[
    EitherT[F, PlayNommDAppFailure, *],
    MerkleTrieState,
    (Account, Transaction.TokenTx.EntrustNFT),
  ] =
    StateT.liftF:
      TransactionRepository[F]
        .get(input)
        .leftMap: e =>
          PlayNommDAppFailure.internal:
            s"Fail to get tx $input: ${e.msg}"
        .subflatMap: txOption =>
          Either.fromOption(
            txOption,
            PlayNommDAppFailure.internal(s"There is no tx of $input"),
          )
        .subflatMap: txWithResult =>
          txWithResult.signedTx.value match
            case ef: Transaction.TokenTx.EntrustNFT if ef.to === account =>
              Right((txWithResult.signedTx.sig.account, ef))
            case _ =>
              Left:
                PlayNommDAppFailure.external:
                  s"Tx $txWithResult is not an entrust nft transaction"
