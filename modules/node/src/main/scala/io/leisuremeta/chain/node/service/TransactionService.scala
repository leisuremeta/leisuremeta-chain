package io.leisuremeta.chain
package node
package service

import cats.{Functor, Monad}
import cats.data.EitherT
import cats.effect.Clock
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*

import api.model.{Signed, Transaction, TransactionWithResult}
import api.model.TransactionWithResult.ops.*
import repository.{BlockRepository, StateRepository, TransactionRepository}

object TransactionService:
  def submit[F[_]
    : Monad: Clock: BlockRepository: LocalGossipService: StateRepository.AccountState.Name: StateRepository.AccountState.Key](
      txs: Seq[Signed.Tx],
  ): EitherT[F, String, Seq[Signed.TxHash]] =
    scribe.info(
      s"LocalGossipService instance: ${summon[LocalGossipService[F]]}",
    )
    for
      txHashes    <- txs.traverse { tx => LocalGossipService[F].onNewTx(tx) }
      localGossip <- EitherT.right[String](LocalGossipService[F].get)
      newTxs = localGossip.newTxs.keySet
      currentTime <- EitherT.right[String](Clock[F].realTimeInstant)
      _ <- EitherT.rightT[F, String](
        scribe.info(s"current localGossip: $localGossip"),
      )
      _ <- EitherT.rightT[F, String](scribe.info(s"current new txs: $newTxs"))
      _ <- EitherT.rightT[F, String](scribe.info(s"current time: $currentTime"))
      result <- LocalGossipService[F].generateNewBlockSuggestion(currentTime)
      _      <- EitherT.rightT[F, String](scribe.info(s"block result: $result"))
    yield txHashes

  def get[F[_]: Functor: TransactionRepository](
      txHash: Signed.TxHash,
  ): EitherT[F, String, Option[TransactionWithResult]] =
    TransactionRepository[F].get(txHash.toResultHashValue).leftMap(_.msg)
