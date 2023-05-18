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

    case mf: Transaction.TokenTx.MintFungibleToken => ???
    case mn: Transaction.TokenTx.MintNFT => ???
    case tf: Transaction.TokenTx.TransferFungibleToken => ???
    case tn: Transaction.TokenTx.TransferNFT => ???
    case bf: Transaction.TokenTx.BurnFungibleToken => ???
    case bn: Transaction.TokenTx.BurnNFT => ???
    case ef: Transaction.TokenTx.EntrustFungibleToken => ???
    case de: Transaction.TokenTx.DisposeEntrustedFungibleToken => ???
    case ef: Transaction.TokenTx.EntrustNFT => ???
    case de: Transaction.TokenTx.DisposeEntrustedNFT => ???
  
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

  def putTokenDefinition[F[_]: Monad: PlayNommState](
      definitionId: TokenDefinitionId,
      definition: TokenDefinition,
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, Unit] =
    PlayNommState[F].token.definition
      .put(definitionId, definition)
      .mapK(PlayNommDAppFailure.mapInternal {
        s"Fail to set token definition of $definitionId"
      })
