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
import lib.merkle.MerkleTrie
import lib.codec.byte.{ByteDecoder, DecodeResult}
import lib.codec.byte.ByteEncoder.ops.*
import lib.datatype.Utf8
import repository.StateRepository
import repository.StateRepository.given

trait UpdateStateWithDaoTx:

  given updateStateWithDaoTx[F[_]: Concurrent: StateRepository.GroupState]
      : UpdateState[F, Transaction.DaoTx] =
    (ms: MerkleState, sig: AccountSignature, tx: Transaction.DaoTx) =>
      tx match
        case cg: Transaction.DaoTx.RegisterDao =>
          ???
