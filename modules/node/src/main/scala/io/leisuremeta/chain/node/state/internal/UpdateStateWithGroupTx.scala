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
  Signed,
  Transaction,
  TransactionWithResult,
}
import lib.merkle.GenericMerkleTrie
import lib.codec.byte.{ByteDecoder, DecodeResult}
import lib.codec.byte.ByteEncoder.ops.*
import lib.datatype.Utf8
import repository.StateRepository
import repository.StateRepository.given

trait UpdateStateWithGroupTx:

  given updateStateWithGroupTx[F[_]: Concurrent: StateRepository.GroupState]
      : UpdateState[F, Transaction.GroupTx] =
    (ms: MerkleState, sig: AccountSignature, tx: Transaction.GroupTx) =>
      tx match
        case cg: Transaction.GroupTx.CreateGroup =>
          if cg.coordinator === sig.account then
            for groupState <- GenericMerkleTrie
                .put(
                  cg.groupId.toBytes.bits,
                  GroupData(cg.name, cg.coordinator),
                )
                .runS(ms.group.groupState)
            yield (
              ms.copy(group = ms.group.copy(groupState = groupState)),
              TransactionWithResult(Signed(sig, cg), None),
            )
          else
            EitherT.leftT(
              s"Account does not match signature: ${cg.coordinator} vs ${sig.account}",
            )
        case ag: Transaction.GroupTx.AddAccounts =>
          for
            groupDataOption <- GenericMerkleTrie
              .get[F, GroupId, GroupData](ag.groupId.toBytes.bits)
              .runA(ms.group.groupState)
            groupData <- EitherT.fromOption[F](
              groupDataOption,
              s"Group does not exist: ${ag.groupId}",
            )
            _ <- EitherT.cond(
              groupData.coordinator === sig.account,
              (),
              s"Account does not match signature: ${groupData.coordinator} vs ${sig.account}",
            )
            groupAccountState <- ag.accounts.toList.foldLeftM(
              ms.group.groupAccountState,
            ) { (state, account) =>
              GenericMerkleTrie
                .put[F, (GroupId, Account), Unit](
                  (ag.groupId, account).toBytes.bits,
                  (),
                )
                .runS(state)
            }
          yield (
            ms.copy(group =
              ms.group.copy(groupAccountState = groupAccountState),
            ),
            TransactionWithResult(Signed(sig, ag), None),
          )
