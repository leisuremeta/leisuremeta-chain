package io.leisuremeta.chain
package node
package dapp

import cats.effect.Concurrent
import cats.data.{EitherT, StateT}

import api.model.{Signed, Transaction, TransactionWithResult}
import repository.{GenericStateRepository, TransactionRepository}
import submodule.*

import GossipDomain.MerkleState

object PlayNommDApp:
  def apply[F[_]: Concurrent: TransactionRepository: PlayNommState: GenericStateRepository.TokenState](
      signedTx: Signed.Tx,
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleState, TransactionWithResult] =
    signedTx.value match
      case rewardTx: Transaction.RewardTx =>
        PlayNommDAppReward(rewardTx, signedTx.sig)

      case _ => ???
