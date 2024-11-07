package io.leisuremeta.chain
package node
package dapp
package submodule

import cats.data.{EitherT, StateT}
import cats.effect.Concurrent
import cats.syntax.all.*

import api.model.{
  Account,
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

    case ud: Transaction.CreatorDaoTx.UpdateCreatorDao =>
      def isModerator(account: Account): StateT[
        EitherT[F, PlayNommDAppFailure, *],
        MerkleTrieState,
        Boolean,
      ] = PlayNommState[F].creatorDao.daoModerators
        .get((ud.id, sig.account))
        .map(_.isDefined)
        .mapK:
          PlayNommDAppFailure.mapInternal:
            s"Failed to get CreatorDaoModerator with id ${ud.id} and account ${sig.account}"

      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        daoInfoOption <- PlayNommState[F].creatorDao.dao
          .get(ud.id)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to get CreatorDao with id ${ud.id}"
        daoInfo <- fromOption(
          daoInfoOption,
          s"CreatorDao with id ${ud.id} does not exist",
        )
        founderOrCoordinator =
          sig.account === daoInfo.founder || sig.account === daoInfo.coordinator
        hasAuth <-
          if founderOrCoordinator then pure(true) else isModerator(sig.account)
        _ <- checkExternal(
          hasAuth,
          s"Account ${sig.account} is not authorized to update CreatorDao with id ${ud.id}",
        )
        daoInfo1 = daoInfo.copy(
          name = ud.name,
          description = ud.description,
        )
        _ <- PlayNommState[F].creatorDao.dao
          .put(ud.id, daoInfo1)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to put CreatorDao with id ${ud.id}"
      yield TransactionWithResult(Signed(sig, ud))(None)

    case dd: Transaction.CreatorDaoTx.DisbandCreatorDao =>
      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        daoInfoOption <- PlayNommState[F].creatorDao.dao
          .get(dd.id)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to get CreatorDao with id ${dd.id}"
        daoInfo <- fromOption(
          daoInfoOption,
          s"CreatorDao with id ${dd.id} does not exist",
        )
        founderOrCoordinator =
          sig.account === daoInfo.founder || sig.account === daoInfo.coordinator
        _ <- checkExternal(
          founderOrCoordinator,
          s"Account ${sig.account} is not authorized to disband DAO ${dd.id}",
        )
        _ <- PlayNommState[F].creatorDao.dao
          .remove(dd.id)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to remove CreatorDao with id ${dd.id}"
      yield TransactionWithResult(Signed(sig, dd))(None)
