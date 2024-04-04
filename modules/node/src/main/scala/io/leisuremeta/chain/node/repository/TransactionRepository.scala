package io.leisuremeta.chain
package node
package repository

import cats.data.EitherT

import api.model.TransactionWithResult
import lib.crypto.Hash
import lib.failure.DecodingFailure
import store.HashStore

trait TransactionRepository[F[_]]:
  def get(
      transactionHash: Hash.Value[TransactionWithResult],
  ): EitherT[F, DecodingFailure, Option[TransactionWithResult]]
  def put(transaction: TransactionWithResult): F[Unit]

object TransactionRepository:

  def apply[F[_]](implicit
      txRepo: TransactionRepository[F],
  ): TransactionRepository[F] = txRepo

  def fromStores[F[_]](implicit
      transctionHashStore: HashStore[F, TransactionWithResult],
  ): TransactionRepository[F] = new TransactionRepository[F]:

    def get(
        transactionHash: Hash.Value[TransactionWithResult],
    ): EitherT[F, DecodingFailure, Option[TransactionWithResult]] =
      transctionHashStore.get(transactionHash)

    def put(transaction: TransactionWithResult): F[Unit] =
      transctionHashStore.put(transaction)
