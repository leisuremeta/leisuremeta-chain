package io.leisuremeta.chain
package node
package dapp
package submodule

import cats.data.{EitherT, StateT}
import cats.effect.Concurrent

import api.model.{
  AccountSignature,
  Signed,
  Transaction,
  TransactionWithResult,
}
import api.model.creator_dao.*
import lib.merkle.MerkleTrieState
import repository.TransactionRepository

object PlayNommDAppCreatorDao:

  def apply[F[_]: Concurrent: PlayNommState: TransactionRepository](
      tx: Transaction.CreatorDaoTx,
      sig: AccountSignature,
  ): StateT[
    EitherT[F, PlayNommDAppFailure, *],
    MerkleTrieState,
    TransactionWithResult,
  ] = tx match
    case cd: Transaction.CreatorDaoTx.CreateCreatorDao =>
      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        daoInfoOption <- PlayNommState[F].creatorDao.dao
          .get(cd.id)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to get CreatorDao with id ${cd.id}"
        _ <- checkExternal(
          daoInfoOption.isEmpty,
          s"CreatorDao with id ${cd.id} already exists",
        )
        daoInfo = CreatorDaoInfo(
          id = cd.id,
          name = cd.name,
          description = cd.description,
          founder = sig.account,
          coordinator = cd.coordinator,
        )
        _ <- PlayNommState[F].creatorDao.dao
          .put(cd.id, daoInfo)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to put CreatorDao with id ${cd.id}"
      yield TransactionWithResult(Signed(sig, cd))(None)
