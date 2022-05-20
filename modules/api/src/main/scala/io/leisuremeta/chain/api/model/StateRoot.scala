package io.leisuremeta.chain
package api.model

import java.time.Instant

import lib.merkle.MerkleTrieNode.MerkleRoot

final case class StateRoot(
    account: StateRoot.AccountStateRoot,
)

object StateRoot:

  def empty: StateRoot = StateRoot(
    account = StateRoot.AccountStateRoot.empty,
  )
  case class AccountStateRoot(
      namesRoot: Option[MerkleRoot[Account, Option[Account]]],
      keyRoot: Option[
        MerkleRoot[(Account, PublicKeySummary), PublicKeyDescription],
      ],
  )
  object AccountStateRoot:
    def empty: AccountStateRoot = AccountStateRoot(None, None)

  case class PublicKeyDescription(
      description: String,
      addedAt: Instant,
  )
