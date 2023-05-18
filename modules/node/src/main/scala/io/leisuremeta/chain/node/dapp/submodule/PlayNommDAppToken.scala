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
import api.model.token.*
import lib.crypto.Hash.ops.*
import lib.datatype.BigNat
import lib.merkle.MerkleTrieState
import GossipDomain.MerkleState

object PlayNommDAppToken:
  def apply[F[_]: Concurrent: PlayNommState](
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

    case tf: Transaction.TokenTx.TransferFungibleToken         => ???
    case tn: Transaction.TokenTx.TransferNFT                   => ???
    case bf: Transaction.TokenTx.BurnFungibleToken             => ???
    case bn: Transaction.TokenTx.BurnNFT                       => ???
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
