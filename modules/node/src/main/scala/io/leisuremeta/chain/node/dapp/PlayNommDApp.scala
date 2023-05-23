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
      case accountTx: Transaction.AccountTx =>
        PlayNommDAppAccount(accountTx, signedTx.sig)
      case groupTx: Transaction.GroupTx =>
        PlayNommDAppGroup(groupTx, signedTx.sig)
      case tokenTx: Transaction.TokenTx =>
        PlayNommDAppToken(tokenTx, signedTx.sig)
      case rewardTx: Transaction.RewardTx =>
        PlayNommDAppReward(rewardTx, signedTx.sig)
      case agendaTx: Transaction.AgendaTx =>
        PlayNommDAppAgenda(agendaTx, signedTx.sig)
