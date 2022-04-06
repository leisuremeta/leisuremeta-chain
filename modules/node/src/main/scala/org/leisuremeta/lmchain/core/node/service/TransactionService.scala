package org.leisuremeta.lmchain.core
package node
package service

import cats.{Functor, Monad}
import cats.data.EitherT
import cats.implicits._

import crypto.Hash.ops._
import model.Signed
import repository.TransactionRepository
import service.LocalGossipService

object TransactionService {

  def get[F[_]: Monad: TransactionRepository: LocalGossipService](
      transactionHash: Signed.TxHash
  ): EitherT[F, String, Option[Signed.Tx]] = for {
    txOption <- TransactionRepository[F].get(transactionHash).leftMap(_.msg)
    txOption1 <- EitherT.right[String](
      LocalGossipService[F].get.map(_.newTxs.get(transactionHash))
    )
  } yield txOption orElse txOption1

  def submit[F[_]: Functor: LocalGossipService](
      tx: Signed.Tx
  ): EitherT[F, String, Signed.TxHash] =
    LocalGossipService[F].onNewTx(tx).map(_ => tx.toHash)
}
