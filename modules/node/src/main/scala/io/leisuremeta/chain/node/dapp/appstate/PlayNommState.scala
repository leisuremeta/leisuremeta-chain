package io.leisuremeta.chain
package node
package dapp
package appstate

import cats.Monad

import api.model.*
import api.model.token.*
import api.model.reward.*
import lib.datatype.Utf8
import lib.merkle.MerkleTrie.NodeStore
import lib.merkle.MerkleTrieNode.MerkleRoot
import lib.application.DAppState
import java.time.Instant

trait PlayNommState[F[_]]:
  def reward: PlayNommState.Reward[F]

object PlayNommState:

  def apply[F[_]: PlayNommState]: PlayNommState[F] = summon

  case class Reward[F[_]](
      dao: DAppState[F, GroupId, DaoInfo],
      accountActivity: DAppState[F, (Instant, Account), Map[Utf8, DaoActivity]],
      tokenReceived: DAppState[F, (Instant, TokenId), Map[Utf8, DaoActivity]],
      accountSnapshot: DAppState[F, Account, Map[Utf8, ActivitySnapshot]],
      tokenSnapshot: DAppState[F, TokenId, Map[Utf8, ActivitySnapshot]],
      ownershipSnapshot: DAppState[F, TokenId, OwnershipSnapshot],
      accountRewarded: DAppState[F, Account, Map[Utf8, ActivityRewardLog]],
      tokenRewarded: DAppState[F, TokenId, Map[Utf8, ActivityRewardLog]],
      ownershipRewarded: DAppState[F, TokenId, Map[Utf8, OwnershipRewardLog]],
  )

  def build[F[_]: Monad: NodeStore]: PlayNommState[F] =
    scribe.info(s"Building PlayNommState... ")

    val playNommState = DAppState.WithCommonPrefix("playnomm")

    new PlayNommState:
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
