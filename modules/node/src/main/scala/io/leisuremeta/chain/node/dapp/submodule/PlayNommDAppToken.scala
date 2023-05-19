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
  AccountData,
  AccountSignature,
  GroupData,
  PublicKeySummary,
  Signed,
  Transaction,
  TransactionWithResult,
}
import api.model.TransactionWithResult.ops.*
import api.model.token.*
import lib.crypto.Hash
import lib.crypto.Hash.ops.*
import lib.datatype.BigNat
import lib.merkle.MerkleTrieState
import repository.TransactionRepository
import GossipDomain.MerkleState
import io.leisuremeta.chain.api.model.Transaction.TokenTx.MintNFT
import io.leisuremeta.chain.api.model.Transaction.TokenTx.TransferNFT
import io.leisuremeta.chain.api.model.Transaction.TokenTx.DisposeEntrustedNFT

object PlayNommDAppToken:
  def apply[F[_]: Concurrent: PlayNommState: TransactionRepository](
      tx: Transaction.TokenTx,
      sig: AccountSignature,
  ): StateT[
    EitherT[F, PlayNommDAppFailure, *],
    MerkleState,
    TransactionWithResult,
  ] = tx match
    case dt: Transaction.TokenTx.DefineToken =>
      val program = for
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
          nftInfo = dt.nftInfo,
        )
        _ <- putTokenDefinition(dt.definitionId, tokenDefinition)
      yield TransactionWithResult(Signed(sig, dt))(None)

      program
        .transformS[MerkleState](_.main, (ms, mts) => (ms.copy(main = mts)))

    case mf: Transaction.TokenTx.MintFungibleToken =>
      val program = for
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

      program
        .transformS[MerkleState](_.main, (ms, mts) => (ms.copy(main = mts)))

    case mn: Transaction.TokenTx.MintNFT =>
      val program = for
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
        nftState = NftState(
          tokenId = mn.tokenId,
          tokenDefinitionId = mn.tokenDefinitionId,
          rarity = mn.rarity,
          weight = tokenDef.nftInfo.get.rarity(mn.rarity),
          currentOwner = mn.output,
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
        .transformS[MerkleState](_.main, (ms, mts) => (ms.copy(main = mts)))

    case tf: Transaction.TokenTx.TransferFungibleToken =>
      val program = for
        _        <- PlayNommDAppAccount.verifySignature(sig, tx)
        tokenDef <- getTokenDefinition(tf.tokenDefinitionId)
        inputAmount <- getInputAmounts(
          tf.inputs.map(_.toResultHashValue),
          sig.account,
        )
        outputAmount = tf.outputs.values.foldLeft(BigNat.Zero)(BigNat.add)
        diff <- fromEitherExternal:
          BigNat.fromBigInt:
            inputAmount.toBigInt - outputAmount.toBigInt
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

      program
        .transformS[MerkleState](_.main, (ms, mts) => (ms.copy(main = mts)))

    case tn: Transaction.TokenTx.TransferNFT =>
      val program = for
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

      program
        .transformS[MerkleState](_.main, (ms, mts) => (ms.copy(main = mts)))

    case bf: Transaction.TokenTx.BurnFungibleToken =>
      val program = for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        tokenDef <- checkMinterAndGetTokenDefinition(
          sig.account,
          bf.definitionId,
        )
        inputAmount <- getInputAmounts(
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

      program
        .transformS[MerkleState](_.main, (ms, mts) => (ms.copy(main = mts)))

    case bn: Transaction.TokenTx.BurnNFT =>
      val program = for
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

      program
        .transformS[MerkleState](_.main, (ms, mts) => (ms.copy(main = mts)))

    case ef: Transaction.TokenTx.EntrustFungibleToken          => ???
    case de: Transaction.TokenTx.DisposeEntrustedFungibleToken => ???
    case ef: Transaction.TokenTx.EntrustNFT                    => ???
    case de: Transaction.TokenTx.DisposeEntrustedNFT           => ???

  def getTokenDefinitionOption[F[_]: Monad: PlayNommState](
      definitionId: TokenDefinitionId,
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, Option[
    TokenDefinition,
  ]] =
    PlayNommState[F].token.definition
      .get(definitionId)
      .mapK(PlayNommDAppFailure.mapInternal {
        s"Fail to get token definition of ${definitionId}"
      })

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

  def getInputAmounts[F[_]: Monad: TransactionRepository: PlayNommState](
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
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, Unit] =
    for
      removeResults <- inputs.toSeq.traverse { txHash =>
        PlayNommState[F].token.fungibleBalance
          .remove((account, definitionId, txHash))
          .mapK(PlayNommDAppFailure.mapInternal {
            s"Fail to remove fingible balance ($account, $definitionId, $txHash)"
          })
      }
      invalidUtxos = inputs.zip(removeResults).filterNot(_._2)
      _ <- checkExternal(
        invalidUtxos.isEmpty,
        s"These utxos are invalid: $invalidUtxos",
      )
    yield ()

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
          df.outputs.get(txWithResult.signedTx.sig.account).getOrElse(BigNat.Zero)
        case or: Transaction.RewardTx.OfferReward =>
          or.outputs.get(txWithResult.signedTx.sig.account).getOrElse(BigNat.Zero)
        case xr: Transaction.RewardTx.ExecuteReward =>
          txWithResult.result.fold(BigNat.Zero):
            case Transaction.RewardTx.ExecuteRewardResult(outputs) =>
              outputs.get(txWithResult.signedTx.sig.account).getOrElse(BigNat.Zero)
            case _ => BigNat.Zero
        case xo: Transaction.RewardTx.ExecuteOwnershipReward =>
          txWithResult.result.fold(BigNat.Zero):
            case Transaction.RewardTx.ExecuteOwnershipRewardResult(outputs) =>
              outputs.get(txWithResult.signedTx.sig.account).getOrElse(BigNat.Zero)
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
        .subflatMap { txOption =>
          Either.fromOption(
            txOption,
            PlayNommDAppFailure.internal(s"There is no tx of $utxoHash"),
          )
        }
        .flatMap: txWithResult =>
          txWithResult.signedTx.value match
            case nb: Transaction.NftBalance =>
              EitherT.pure:
                nb match
                  case mn: MintNFT              => mn.tokenId
                  case tn: TransferNFT          => tn.tokenId
                  case den: DisposeEntrustedNFT => den.tokenId
            case _ =>
              EitherT.leftT:
                PlayNommDAppFailure.external(
                  s"Tx $txWithResult is not a nft balance",
                )
