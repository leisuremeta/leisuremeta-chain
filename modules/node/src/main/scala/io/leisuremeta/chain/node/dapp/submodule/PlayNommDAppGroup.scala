package io.leisuremeta.chain
package node
package dapp
package submodule

import cats.data.{EitherT, StateT}
import cats.effect.Concurrent
import cats.syntax.all.*

import api.model.{
  Account,
  AccountData,
  AccountSignature,
  GroupData,
  PublicKeySummary,
  Signed,
  Transaction,
  TransactionWithResult,
}
import GossipDomain.MerkleState

object PlayNommDAppGroup:
  def apply[F[_]: Concurrent: PlayNommState](
      tx: Transaction.GroupTx,
      sig: AccountSignature,
  ): StateT[
    EitherT[F, PlayNommDAppFailure, *],
    MerkleState,
    TransactionWithResult,
  ] = tx match
    case cg: Transaction.GroupTx.CreateGroup =>
      val program = for
        accountData <- PlayNommDAppAccount.verifySignature[F](sig, cg)
        _ <- checkExternal[F](
          cg.coordinator === sig.account,
          s"Account does not match signature: ${cg.coordinator} vs ${sig.account}",
        )
        _ <- PlayNommState[F].group.group
          .put(cg.groupId, GroupData(cg.name, cg.coordinator))
          .mapK:
            PlayNommDAppFailure
              .mapInternal[F](s"Failed to create group: ${cg.groupId}")
      yield TransactionWithResult(Signed(sig, cg), None)

      program
        .transformS[MerkleState](_.main, (ms, mts) => (ms.copy(main = mts)))
    case aa: Transaction.GroupTx.AddAccounts =>
      val program = for
        groupDataOption <- PlayNommState[F].group.group
          .get(aa.groupId)
          .mapK:
            PlayNommDAppFailure
              .mapInternal[F](s"Failed to get group ${aa.groupId}")
        groupData <- fromOption(
          groupDataOption,
          s"Group does not exist: ${aa.groupId}",
        )
        _ <- checkExternal[F](
          groupData.coordinator === sig.account,
          s"Account does not match signature: ${groupData.coordinator} vs ${sig.account}",
        )
        _ <- PlayNommDAppAccount.verifySignature[F](sig, aa)
        _ <- aa.accounts.toList.traverse: account =>
          PlayNommState[F].group.groupAccount
            .put((aa.groupId, account), ())
            .mapK:
              PlayNommDAppFailure.mapInternal[F]:
                s"Failed to add an account $account to group: ${aa.groupId}"
      yield TransactionWithResult(Signed(sig, aa), None)

      program
        .transformS[MerkleState](_.main, (ms, mts) => (ms.copy(main = mts)))
