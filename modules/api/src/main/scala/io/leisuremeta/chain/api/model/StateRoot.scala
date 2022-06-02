package io.leisuremeta.chain
package api.model

import java.time.Instant

import cats.Eq

import lib.merkle.MerkleTrieNode.MerkleRoot

final case class StateRoot(
    account: StateRoot.AccountStateRoot,
    group: StateRoot.GroupStateRoot,
)

object StateRoot:

  def empty: StateRoot = StateRoot(
    account = StateRoot.AccountStateRoot.empty,
    group = StateRoot.GroupStateRoot.empty,
  )
  case class AccountStateRoot(
      namesRoot: Option[MerkleRoot[Account, Option[Account]]],
      keyRoot: Option[
        MerkleRoot[(Account, PublicKeySummary), PublicKeySummary.Info],
      ],
  )
  object AccountStateRoot:
    def empty: AccountStateRoot = AccountStateRoot(None, None)

  case class GroupStateRoot(
      groupRoot: Option[MerkleRoot[GroupId, GroupData]],
      groupAccountRoot: Option[MerkleRoot[(GroupId, Account), Unit]],
  )
  object GroupStateRoot:
    def empty: GroupStateRoot = GroupStateRoot(None, None)

  given eqStateRoot: Eq[StateRoot] = Eq.fromUniversalEquals
