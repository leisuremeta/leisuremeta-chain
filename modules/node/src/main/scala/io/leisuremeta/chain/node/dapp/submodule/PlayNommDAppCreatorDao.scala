package io.leisuremeta.chain
package node
package dapp
package submodule

import cats.Functor
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
        daoDataOption <- PlayNommState[F].creatorDao.dao
          .get(cd.id)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to get CreatorDao with id ${cd.id}"
        _ <- checkExternal(
          daoDataOption.isEmpty,
          s"CreatorDao with id ${cd.id} already exists",
        )
        daoData = CreatorDaoData(
          id = cd.id,
          name = cd.name,
          description = cd.description,
          founder = sig.account,
          coordinator = cd.coordinator,
        )
        _ <- PlayNommState[F].creatorDao.dao
          .put(cd.id, daoData)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to put CreatorDao with id ${cd.id}"
      yield TransactionWithResult(Signed(sig, cd))(None)

    case ud: Transaction.CreatorDaoTx.UpdateCreatorDao =>
      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        daoDataOption <- PlayNommState[F].creatorDao.dao
          .get(ud.id)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to get CreatorDao with id ${ud.id}"
        daoData <- fromOption(
          daoDataOption,
          s"CreatorDao with id ${ud.id} does not exist",
        )
        founderOrCoordinator =
          sig.account === daoData.founder || sig.account === daoData.coordinator
        hasAuth <-
          if founderOrCoordinator then pure(true)
          else isModerator(ud.id, sig.account)
        _ <- checkExternal(
          hasAuth,
          s"Account ${sig.account} is not authorized to update CreatorDao with id ${ud.id}",
        )
        daoData1 = daoData.copy(
          name = ud.name,
          description = ud.description,
        )
        _ <- PlayNommState[F].creatorDao.dao
          .put(ud.id, daoData1)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to put CreatorDao with id ${ud.id}"
      yield TransactionWithResult(Signed(sig, ud))(None)

    case dd: Transaction.CreatorDaoTx.DisbandCreatorDao =>
      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        daoDataOption <- PlayNommState[F].creatorDao.dao
          .get(dd.id)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to get CreatorDao with id ${dd.id}"
        daoData <- fromOption(
          daoDataOption,
          s"CreatorDao with id ${dd.id} does not exist",
        )
        founderOrCoordinator =
          sig.account === daoData.founder || sig.account === daoData.coordinator
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

    case rc: Transaction.CreatorDaoTx.ReplaceCoordinator =>
      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        daoDataOption <- PlayNommState[F].creatorDao.dao
          .get(rc.id)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to get CreatorDao with id ${rc.id}"
        daoData <- fromOption(
          daoDataOption,
          s"CreatorDao with id ${rc.id} does not exist",
        )
        isCurrentCoordinator = sig.account === daoData.coordinator
        _ <- checkExternal(
          isCurrentCoordinator,
          s"Only the current Coordinator can replace the Coordinator for DAO ${rc.id}",
        )
        updatedDaoData = daoData.copy(coordinator = rc.newCoordinator)
        _ <- PlayNommState[F].creatorDao.dao
          .put(rc.id, updatedDaoData)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to put CreatorDao with id ${rc.id}"
      yield TransactionWithResult(Signed(sig, rc))(None)

    case am: Transaction.CreatorDaoTx.AddMembers =>
      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        daoDataOption <- PlayNommState[F].creatorDao.dao
          .get(am.id)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to get CreatorDao with id ${am.id}"
        daoData <- fromOption(
          daoDataOption,
          s"CreatorDao with id ${am.id} does not exist",
        )
        founderOrCoordinator =
          sig.account === daoData.founder || sig.account === daoData.coordinator
        hasAuth <-
          if founderOrCoordinator then pure(true)
          else isModerator(am.id, sig.account)
        _ <- checkExternal(
          hasAuth,
          s"Account ${sig.account} is not authorized to add members to DAO ${am.id}",
        )
        _ <- am.members.toSeq.traverse: member =>
          PlayNommState[F].creatorDao.daoMembers
            .put((am.id, member), ())
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Failed to put CreatorDaoMember with id ${am.id} and member ${member}"
      yield TransactionWithResult(Signed(sig, am))(None)

    case rm: Transaction.CreatorDaoTx.RemoveMembers =>
      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        daoDataOption <- PlayNommState[F].creatorDao.dao
          .get(rm.id)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to get CreatorDao with id ${rm.id}"
        daoData <- fromOption(
          daoDataOption,
          s"CreatorDao with id ${rm.id} does not exist",
        )
        founderOrCoordinator =
          sig.account === daoData.founder || sig.account === daoData.coordinator
        hasAuth <-
          if founderOrCoordinator then pure(true)
          else isModerator(rm.id, sig.account)
        _ <- checkExternal(
          hasAuth,
          s"Account ${sig.account} is not authorized to remove members from DAO ${rm.id}",
        )
        _ <- rm.members.toSeq.traverse: member =>
          PlayNommState[F].creatorDao.daoMembers
            .remove((rm.id, member))
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Failed to remove CreatorDaoMember with id ${rm.id} and member ${member}"
      yield TransactionWithResult(Signed(sig, rm))(None)

    case pm: Transaction.CreatorDaoTx.PromoteModerators =>
      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        daoDataOption <- PlayNommState[F].creatorDao.dao
          .get(pm.id)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to get CreatorDao with id ${pm.id}"
        daoData <- fromOption(
          daoDataOption,
          s"CreatorDao with id ${pm.id} does not exist",
        )
        hasPermission = daoData.founder == sig.account || daoData.coordinator == sig.account
        _ <- checkExternal(
          hasPermission,
          s"Account ${sig.account} does not have permission to promote moderators in DAO ${pm.id}",
        )
        _ <- pm.members.toSeq.traverse: member =>
          PlayNommState[F].creatorDao.daoModerators
            .put((pm.id, member), ())
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Failed to put CreatorDaoModerator with id ${pm.id} and member ${member}"
      yield TransactionWithResult(Signed(sig, pm))(None)

    case dm: Transaction.CreatorDaoTx.DemoteModerators =>
      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        daoDataOption <- PlayNommState[F].creatorDao.dao
          .get(dm.id)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to get CreatorDao with id ${dm.id}"
        daoData <- fromOption(
          daoDataOption,
          s"CreatorDao with id ${dm.id} does not exist",
        )
        hasPermission = daoData.founder == sig.account || daoData.coordinator == sig.account
        _ <- checkExternal(
          hasPermission,
          s"Account ${sig.account} does not have permission to demote moderators in DAO ${dm.id}",
        )
        _ <- dm.members.toSeq.traverse: member =>
          PlayNommState[F].creatorDao.daoModerators
            .remove((dm.id, member))
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Failed to remove CreatorDaoModerator with id ${dm.id} and member ${member}"
      yield TransactionWithResult(Signed(sig, dm))(None)

def isModerator[F[_]: Functor: PlayNommState](id: CreatorDaoId, account: Account): StateT[
  EitherT[F, PlayNommDAppFailure, *],
  MerkleTrieState,
  Boolean,
] = PlayNommState[F].creatorDao.daoModerators
  .get((id, account))
  .map(_.isDefined)
  .mapK:
    PlayNommDAppFailure.mapInternal:
      s"Failed to get CreatorDaoModerator with id ${id} and account ${account}"
