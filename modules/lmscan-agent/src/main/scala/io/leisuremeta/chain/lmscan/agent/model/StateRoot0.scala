package io.leisuremeta.chain
package lmscan.agent.model

import api.model.*

import java.time.Instant

import cats.Eq

import lib.crypto.Hash
import lib.merkle.GenericMerkleTrieNode.{MerkleRoot => GenericMerkleRoot}
import lib.merkle.MerkleTrieNode.MerkleRoot
import account.EthAddress
import reward.*
import token.*

final case class StateRoot0(
    // main: Option[MerkleRoot],
    account: StateRoot.AccountStateRoot,
    group: StateRoot.GroupStateRoot,
    token: StateRoot.TokenStateRoot,
    reward: StateRoot.RewardStateRoot,
)

object StateRoot0:

  def empty: StateRoot0 = StateRoot0(
    // main = None,
    account = StateRoot.AccountStateRoot.empty,
    group = StateRoot.GroupStateRoot.empty,
    token = StateRoot.TokenStateRoot.empty,
    reward = StateRoot.RewardStateRoot.empty,
  )
  
  given eqStateRoot: Eq[StateRoot0] = Eq.fromUniversalEquals
