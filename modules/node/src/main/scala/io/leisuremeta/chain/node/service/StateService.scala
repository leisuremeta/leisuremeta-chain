package io.leisuremeta.chain
package node
package service

import cats.data.EitherT
import cats.effect.Concurrent

import GossipDomain.MerkleState
import api.model.{Signed, Transaction, TransactionWithResult}
import repository.{StateRepository, TransactionRepository}
import state.UpdateState

object StateService:

  def updateStateWithTx[F[_]
    : Concurrent: StateRepository.AccountState: StateRepository.GroupState: TransactionRepository](
      state: MerkleState,
      signedTx: Signed.Tx,
  ): EitherT[F, String, (MerkleState, TransactionWithResult)] =
    scribe.debug(s"Updating state with tx: $signedTx")
    signedTx.value match
      case tx: Transaction.AccountTx =>
        UpdateState[F, Transaction.AccountTx](state, signedTx.sig, tx)
      case tx: Transaction.GroupTx =>
        UpdateState[F, Transaction.GroupTx](state, signedTx.sig, tx)
