package io.leisuremeta.chain
package node
package dapp
package submodule

import cats.Monad
import cats.data.{EitherT, StateT}

import api.model.{AccountSignature, Transaction, TransactionWithResult}
import repository.TransactionRepository

import GossipDomain.MerkleState
import PlayNommDAppUtil.*

object PlayNommDAppReward:
  def apply[F[_]: Monad: TransactionRepository: PlayNommState](
      tx: Transaction.RewardTx,
      sig: AccountSignature
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleState, TransactionWithResult] = tx match
    case tx: Transaction.RewardTx.RegisterDao => ???
    case tx: Transaction.RewardTx.UpdateDao => ???
    case tx: Transaction.RewardTx.RecordActivity => ???
    case tx: Transaction.RewardTx.BuildSnapshot => ???
    case tx: Transaction.RewardTx.ExecuteAccountReward => ???
    case tx: Transaction.RewardTx.ExecuteTokenReward => ???
    case tx: Transaction.RewardTx.ExecuteOwnershipReward => ???
