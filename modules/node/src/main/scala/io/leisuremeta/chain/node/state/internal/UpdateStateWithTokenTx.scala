package io.leisuremeta.chain
package node
package state
package internal

import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.eq.given
import cats.syntax.foldable.given

import scodec.bits.BitVector

import GossipDomain.MerkleState
import UpdateState.*
import api.model.{
  Account,
  AccountSignature,
  GroupData,
  GroupId,
  PublicKeySummary,
  Signed,
  Transaction,
  TransactionWithResult,
}
import api.model.token.{TokenDefinition, TokenDefinitionId}
import lib.merkle.MerkleTrie
import lib.codec.byte.{ByteDecoder, DecodeResult}
import lib.codec.byte.ByteEncoder.ops.*
import lib.crypto.Hash
import lib.crypto.Hash.ops.*
import lib.datatype.{BigNat, Utf8}
import repository.StateRepository
import repository.StateRepository.given

trait UpdateStateWithTokenTx:

  given updateStateWithTokenTx[F[_]: Concurrent: StateRepository.TokenState: StateRepository.GroupState: StateRepository.AccountState]
      : UpdateState[F, Transaction.TokenTx] =
    (ms: MerkleState, sig: AccountSignature, tx: Transaction.TokenTx) =>
      tx match
        case dt: Transaction.TokenTx.DefineToken =>
          for
            tokenDefinitionOption <- MerkleTrie
              .get[F, TokenDefinitionId, TokenDefinition](
                dt.definitionId.toBytes.bits,
              )
              .runA(ms.token.tokenDefinitionState)
            _ <- EitherT.cond(
              tokenDefinitionOption.isEmpty,
              (),
              s"Token definition ${dt.definitionId} already exists",
            )
            tokenDefinition = TokenDefinition(
              id = dt.definitionId,
              name = dt.name,
              symbol = dt.symbol,
              adminGroup = dt.minterGroup,
              totalAmount = BigNat.Zero,
              nftInfo = dt.nftInfo,
            )
            tokenDefinitionState <- MerkleTrie
              .put[F, TokenDefinitionId, TokenDefinition](
                dt.definitionId.toBytes.bits,
                tokenDefinition,
              )
              .runS(ms.token.tokenDefinitionState)
          yield
            scribe.info(s"===> new Token Definition state: $tokenDefinitionState")
            (
              ms.copy(token =
                ms.token.copy(tokenDefinitionState = tokenDefinitionState),
              ),
              TransactionWithResult(Signed(sig, tx), None),
            )
        case mf: Transaction.TokenTx.MintFungibleToken =>
          for
            tokenDefinitionOption <- MerkleTrie
              .get[F, TokenDefinitionId, TokenDefinition](
                mf.definitionId.toBytes.bits,
              )
              .runA(ms.token.tokenDefinitionState)
            tokenDefinition <- EitherT.fromOption[F](
              tokenDefinitionOption,
              s"Token definition ${mf.definitionId} does not exist"
            )
            adminGroupId <- EitherT.fromOption[F](
              tokenDefinition.adminGroup,
              s"Admin group does not exist in token $tokenDefinition"
            )
            groupAccountOption <- MerkleTrie
              .get[F, (GroupId, Account), Unit]((adminGroupId, sig.account).toBytes.bits)
              .runA(ms.group.groupAccountState)
            _ <- EitherT.cond(
              groupAccountOption.nonEmpty,
              (),
              s"Account ${sig.account} is not a member of admin group $adminGroupId",
            )
            pubKeySummary <- EitherT.fromEither[F](recoverSignature(mf, sig.sig))
            accountPubKeyOption <- MerkleTrie
              .get[F, (Account, PublicKeySummary), PublicKeySummary.Info](
                (sig.account, pubKeySummary).toBytes.bits,
              )
              .runA(ms.account.keyState)
            _ <- EitherT.cond(
              accountPubKeyOption.nonEmpty,
              (),
              s"Account ${sig.account} does not have public key summary $pubKeySummary",
            )
            txWithResult = TransactionWithResult(Signed(sig, tx), None)
            items = mf.outputs.map{ case (account, _) => (account, mf.definitionId, txWithResult.toHash)}
            fungibleBalanceState <- items.toList.foldLeftM(ms.token.fungibleBalanceState){ case (state, item) =>
              MerkleTrie
                .put[F, (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]), Unit](item.toBytes.bits, ())
                .runS(state)
            }
          yield (ms.copy(token = ms.token.copy(fungibleBalanceState = fungibleBalanceState)), txWithResult)
          