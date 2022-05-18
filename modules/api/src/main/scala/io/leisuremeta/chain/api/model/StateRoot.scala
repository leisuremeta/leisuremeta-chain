package io.leisuremeta.chain
package api.model

import java.time.Instant

import lib.merkle.MerkleTrieNode.MerkleRoot

final case class StateRoot(
    account: StateRoot.AccountStateRoot,
)

object StateRoot:
  case class AccountStateRoot(
      namesRoot: Option[MerkleRoot[Account, Option[Account]]],
      keyRoot: Option[
        MerkleRoot[(Account, PublicKeySummary), PublicKeyDescription],
      ],
  )

  case class PublicKeyDescription(
      description: String,
      addedAt: Instant,
  )
