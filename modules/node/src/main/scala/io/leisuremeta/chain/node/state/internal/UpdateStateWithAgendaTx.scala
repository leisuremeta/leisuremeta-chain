package io.leisuremeta.chain
package node
package state
package internal

import java.time.Instant

import cats.data.{EitherT, StateT}
import cats.effect.Concurrent
import cats.syntax.either.given
import cats.syntax.eq.given
import cats.syntax.foldable.given
import cats.syntax.traverse.given

import fs2.Stream
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
import api.model.token.*
import api.model.TransactionWithResult.ops.*
import lib.merkle.{GenericMerkleTrie, GenericMerkleTrieState}
import lib.codec.byte.{ByteDecoder, DecodeResult}
import lib.codec.byte.ByteEncoder.ops.*
import lib.crypto.Hash
import lib.crypto.Hash.ops.*
import lib.datatype.{BigNat, Utf8}
import repository.{BlockRepository, GenericStateRepository, TransactionRepository}
import repository.GenericStateRepository.given
import service.StateReadService

trait UpdateStateWithAgendaTx:

  given updateStateWithAgendaTx[F[_]
    : Concurrent: GenericStateRepository.TokenState: GenericStateRepository.AccountState: BlockRepository: TransactionRepository]
      : UpdateState[F, Transaction.AgendaTx] =
    (ms: MerkleState, sig: AccountSignature, tx: Transaction.AgendaTx) =>
      tx match
        case ssa: Transaction.AgendaTx.SuggestSimpleAgenda =>
          EitherT.pure((ms, TransactionWithResult(Signed[Transaction](sig, tx), None)))
        case ssa: Transaction.AgendaTx.VoteSimpleAgenda =>
          for
            txWithResultOption <- TransactionRepository[F].get(ssa.agendaTxHash).leftMap(_.msg)
            txWithResult <- EitherT.fromOption(txWithResultOption, "AgendaTx not found")
            agendaTx <- txWithResult.signedTx.value match
              case ssa: Transaction.AgendaTx.SuggestSimpleAgenda => EitherT.pure(ssa)
              case _ => EitherT.leftT(s"AgendaTx ${ssa.agendaTxHash} is not SuggestSimpleAgenda")
            freeBalance <- EitherT.right(StateReadService.getFreeBalance[F](sig.account))
            entrustBalance <- EitherT.right(StateReadService.getEntrustBalance[F](sig.account))
            votingAmount = BigNat.add(
              freeBalance.get(agendaTx.votingToken).fold(BigNat.Zero)(_.totalAmount),
              entrustBalance.get(agendaTx.votingToken).fold(BigNat.Zero)(_.totalAmount),
            )
            txResult = Transaction.AgendaTx.VoteSimpleAgendaResult(votingAmount)
          yield (ms, TransactionWithResult(Signed[Transaction](sig, tx), Some(txResult)))
