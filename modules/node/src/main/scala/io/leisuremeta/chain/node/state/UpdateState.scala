package io.leisuremeta.chain
package node
package state

import cats.data.EitherT

import GossipDomain.MerkleState
import api.model.{
  AccountSignature,
  PublicKeySummary,
  Transaction,
  TransactionWithResult,
}
import lib.crypto.Signature
import lib.crypto.Hash.ops.*
import lib.crypto.Recover.ops.*
import internal.*

trait UpdateState[F[_], T <: Transaction]:
  def apply(
      ms: MerkleState,
      sig: AccountSignature,
      tx: T,
  ): EitherT[F, String, (MerkleState, TransactionWithResult)]

object UpdateState
    extends UpdateStateWithAccountTx
    with UpdateStateWithGroupTx
    with UpdateStateWithTokenTx
    with UpdateStateWithDaoTx
    with UpdateStateWithRandomOfferingTx:
      
  def apply[F[_], T <: Transaction](using
      ev: UpdateState[F, T],
  ): UpdateState[F, T] = ev

  def recoverSignature(
      tx: Transaction,
      sig: Signature,
  ): Either[String, PublicKeySummary] = tx.toHash
    .recover(sig)
    .toRight(
      s"Cannot recover public key from signature: $sig and transaction: $tx",
    )
    .map(_.toHash)
    .map(PublicKeySummary.fromPublicKeyHash)
