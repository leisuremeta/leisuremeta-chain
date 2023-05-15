package io.leisuremeta.chain
package node
package dapp

import cats.Monad

import api.model.*
import api.model.{Account => AccountM}
import api.model.account.*
import api.model.token.*
import api.model.reward.*
import lib.datatype.Utf8
import lib.merkle.MerkleTrie.NodeStore
import lib.merkle.MerkleTrieNode.MerkleRoot
import lib.application.DAppState
import java.time.Instant

trait PlayNommState[F[_]]:
  def account: PlayNommState.Account[F]
  def reward: PlayNommState.Reward[F]

object PlayNommState:

  def apply[F[_]: PlayNommState]: PlayNommState[F] = summon

  case class Account[F[_]](
      name: DAppState[F, AccountM, AccountData],
      key: DAppState[F, (AccountM, PublicKeySummary), PublicKeySummary.Info],
      eth: DAppState[F, EthAddress, AccountM],
  )

  case class Reward[F[_]](
      dao: DAppState[F, GroupId, DaoInfo],
      accountActivity: DAppState[F, (AccountM, Instant), Seq[ActivityLog]],
      tokenReceived: DAppState[F, (TokenId, Instant), Seq[ActivityLog]],
      accountSnapshot: DAppState[F, AccountM, ActivitySnapshot],
      tokenSnapshot: DAppState[F, TokenId, ActivitySnapshot],
      ownershipSnapshot: DAppState[F, TokenId, OwnershipSnapshot],
      accountRewarded: DAppState[F, AccountM, ActivityRewardLog],
      tokenRewarded: DAppState[F, TokenId, ActivityRewardLog],
      ownershipRewarded: DAppState[F, TokenId, OwnershipRewardLog],
  )

  def build[F[_]: Monad: NodeStore]: PlayNommState[F] =
    scribe.info(s"Building PlayNommState... ")

    val playNommState = DAppState.WithCommonPrefix("playnomm")

    new PlayNommState:
      val account: Account[F] = Account[F](
        name = playNommState.ofName("name"),
        key = playNommState.ofName("key"),
        eth = playNommState.ofName("eth"),
      )

      val reward: Reward[F] = Reward[F](
        dao = playNommState.ofName("dao"),
        accountActivity = playNommState.ofName("account-activity"),
        tokenReceived = playNommState.ofName("token-received"),
        accountSnapshot = playNommState.ofName("account-snapshot"),
        tokenSnapshot = playNommState.ofName("token-snapshot"),
        ownershipSnapshot = playNommState.ofName("ownership-snapshot"),
        accountRewarded = playNommState.ofName("account-rewarded"),
        tokenRewarded = playNommState.ofName("token-rewarded"),
        ownershipRewarded = playNommState.ofName("ownership-rewarded"),
      )
