package org.leisuremeta.lmchain.core
package node
package repository

import cats.data.EitherT

import model.Signed
import store.HashStore
import failure.DecodingFailure

trait TransactionRepository[F[_]] {
  def get(
      transactionHash: Signed.TxHash
  ): EitherT[F, DecodingFailure, Option[Signed.Tx]]
  def put(transaction: Signed.Tx): F[Unit]
}

object TransactionRepository {

  def apply[F[_]](implicit
      txRepo: TransactionRepository[F]
  ): TransactionRepository[F] = txRepo

  def fromStores[F[_]](implicit
      transctionHashStore: HashStore[F, Signed.Tx]
  ): TransactionRepository[F] = new TransactionRepository[F] {

    def get(
        transactionHash: Signed.TxHash
    ): EitherT[F, DecodingFailure, Option[Signed.Tx]] =
      transctionHashStore.get(transactionHash)

    def put(transaction: Signed.Tx): F[Unit] =
      transctionHashStore.put(transaction)
  }
}
