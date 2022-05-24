package io.leisuremeta.chain
package node
package service

import cats.data.EitherT

import api.model.{Signed, Transaction}
import repository.{BlockRepository, StateRepository}

object TransactionService:
  def submit[F[_]
    : cats.Monad: BlockRepository: LocalGossipService: StateRepository.AccountState.Name: StateRepository.AccountState.Key](
      tx: Signed.Tx,
  ): EitherT[F, String, Signed.TxHash] =
    scribe.info(s"LocalGossipService instance: ${summon[LocalGossipService[F]]}")
    LocalGossipService[F].onNewTx(tx)
