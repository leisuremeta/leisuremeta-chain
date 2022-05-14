package io.leisuremeta.chain.lib.merkle

import MerkleTrieNode.MerkleRoot

final case class MerkleTrieState[K, V](
    root: Option[MerkleRoot[K, V]],
    base: Option[MerkleRoot[K, V]],
    diff: MerkleTrieStateDiff[K, V],
)
object MerkleTrieState:
  def empty[K, V]: MerkleTrieState[K, V] = MerkleTrieState(
    None,
    None,
    MerkleTrieStateDiff.empty[K, V],
  )
  def fromRoot[K, V](root: MerkleRoot[K, V]): MerkleTrieState[K, V] =
    MerkleTrieState[K, V](
      root = Some(root),
      base = Some(root),
      diff = MerkleTrieStateDiff.empty[K, V],
    )
