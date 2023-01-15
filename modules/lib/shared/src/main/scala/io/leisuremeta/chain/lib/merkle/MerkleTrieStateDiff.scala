package io.leisuremeta.chain.lib.merkle

import MerkleTrieNode.MerkleHash

opaque type MerkleTrieStateDiff = Map[MerkleHash, (MerkleTrieNode, Int)]

object MerkleTrieStateDiff:

  def apply(
      map: Map[MerkleHash, (MerkleTrieNode, Int)],
  ): MerkleTrieStateDiff = map

  def empty: MerkleTrieStateDiff = Map.empty

  extension (diff: MerkleTrieStateDiff)
    def get(hash: MerkleHash): Option[MerkleTrieNode] =
      diff.get(hash).flatMap {
        case (node, count) if count > 0 => Some(node)
        case _                          => None
      }

    def foreach(f: (MerkleHash, MerkleTrieNode) => Unit): Unit =
      for
        diffItem <- diff
        (hash, (node, count)) = diffItem if count > 0
      yield f(hash, node)
      ()

    def add(
        hash: MerkleHash,
        node: MerkleTrieNode,
    ): MerkleTrieStateDiff =
      diff.get(hash).fold(diff + (hash -> (node, 1))) {
        case (node, -1)    => diff - hash
        case (node, count) => diff + (hash -> (node, count + 1))
      }

    def remove(
        hash: MerkleHash,
        node: MerkleTrieNode,
    ): MerkleTrieStateDiff =
      diff.get(hash).fold(diff + (hash -> (node, -1))) {
        case (node, 1)     => diff - hash
        case (node, count) => diff + (hash -> (node, count - 1))
      }

    def toList: List[(MerkleHash, (MerkleTrieNode, Int))] = diff.toList

    def toMap: Map[MerkleHash, (MerkleTrieNode, Int)] = diff
  end extension
