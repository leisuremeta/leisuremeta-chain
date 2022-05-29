package io.leisuremeta.chain
package node
package service

import cats.{Functor, Monad}
import cats.data.EitherT
import cats.effect.Clock
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import api.model.{Signed, Transaction, TransactionWithResult}
import api.model.TransactionWithResult.ops.*
import repository.{BlockRepository, StateRepository, TransactionRepository}

object TransactionService:
  def submit[F[_]
    : Monad: Clock: BlockRepository: LocalGossipService: StateRepository.AccountState.Name: StateRepository.AccountState.Key](
      tx: Signed.Tx,
  ): EitherT[F, String, Signed.TxHash] =
    scribe.info(
      s"LocalGossipService instance: ${summon[LocalGossipService[F]]}",
    )
    for
      txHash      <- LocalGossipService[F].onNewTx(tx)
      localGossip <- EitherT.right[String](LocalGossipService[F].get)
      txHashes = localGossip.newTxs.keySet
      currentTime <- EitherT.right[String](Clock[F].realTimeInstant)
      _ <- EitherT.rightT[F, String](scribe.info(s"current localGossip: $localGossip"))
      _ <- EitherT.rightT[F, String](scribe.info(s"current txHashes: $txHashes"))
      _ <- EitherT.rightT[F, String](scribe.info(s"current time: $currentTime"))
      result <- LocalGossipService[F].generateNewBlockSuggestion(currentTime)
      _ <- EitherT.rightT[F, String](scribe.info(s"block result: $result"))

    yield txHash

  def get[F[_]: Functor: TransactionRepository](txHash: Signed.TxHash): EitherT[F, String, Option[TransactionWithResult]] =
    TransactionRepository[F].get(txHash.toResultHashValue).leftMap(_.msg)
  