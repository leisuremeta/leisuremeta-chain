package io.leisuremeta.chain
package node
package dapp
package submodule

import cats.Monad
import cats.data.{EitherT, StateT}
import cats.syntax.traverse.*

import api.model.{AccountSignature, Signed, Transaction, TransactionWithResult}
import api.model.reward.ActivityLog
import lib.crypto.Hash.ops.*
import lib.merkle.MerkleTrieState
import repository.TransactionRepository

import GossipDomain.MerkleState
import PlayNommDAppUtil.*

object PlayNommDAppReward:
  def apply[F[_]: Monad: TransactionRepository: PlayNommState](
      tx: Transaction.RewardTx,
      sig: AccountSignature,
  ): StateT[
    EitherT[F, PlayNommDAppFailure, *],
    MerkleState,
    TransactionWithResult,
  ] = tx match
    case tx: Transaction.RewardTx.RegisterDao => ???
    case tx: Transaction.RewardTx.UpdateDao   => ???
    case tx: Transaction.RewardTx.RecordActivity =>
      val txResult = TransactionWithResult(Signed(sig, tx), None)
      val txHash   = txResult.toHash
      val program: StateT[
        EitherT[F, PlayNommDAppFailure, *],
        MerkleTrieState,
        TransactionWithResult,
      ] =
        for
          _ <- tx.userActivity.toList.traverse { case (account, activities) =>
            val logs = activities.map { a =>
              ActivityLog(a.point, a.description, txHash)
            }
            PlayNommState[F].reward.accountActivity
              .put((account, tx.timestamp), logs)
              .mapK {
                PlayNommDAppFailure.mapInternal {
                  s"Fail to put account activity in $txHash"
                }
              }
          }
          _ <- tx.tokenReceived.toList.traverse { case (account, activities) =>
            val logs = activities.map { a =>
              ActivityLog(a.point, a.description, txHash)
            }
            PlayNommState[F].reward.tokenReceived
              .put((account, tx.timestamp), logs)
              .mapK {
                PlayNommDAppFailure.mapInternal {
                  s"Fail to put account activity in $txHash"
                }
              }
          }
        yield txResult

      program
        .transformS[MerkleState](_.main, (ms, mts) => (ms.copy(main = mts)))

    case tx: Transaction.RewardTx.BuildSnapshot          => ???
    case tx: Transaction.RewardTx.ExecuteAccountReward   => ???
    case tx: Transaction.RewardTx.ExecuteTokenReward     => ???
    case tx: Transaction.RewardTx.ExecuteOwnershipReward => ???
