package io.leisuremeta.chain
package node
package dapp

import cats.Monad
import cats.data.{EitherT, StateT}

import api.model.{Signed, Transaction, TransactionWithResult}
import repository.TransactionRepository
import submodule.*

import GossipDomain.MerkleState

object PlayNommDApp:
  def apply[F[_]: Monad: TransactionRepository: PlayNommState](
      signedTx: Signed.Tx,
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleState, TransactionWithResult] =
    signedTx.value match
      case rewardTx: Transaction.RewardTx =>
        PlayNommDAppReward(rewardTx, signedTx.sig)

      case _ => ???
