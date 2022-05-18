package io.leisuremeta.chain.lib.merkle

import MerkleTrieNode.MerkleHash

opaque type MerkleTrieStateDiff[K, V] =
  Map[MerkleHash[K, V], (MerkleTrieNode[K, V], Int)]

object MerkleTrieStateDiff:

  def empty[K, V]: MerkleTrieStateDiff[K, V] = Map.empty

  extension [K, V](diff: MerkleTrieStateDiff[K, V])
    def get(hash: MerkleHash[K, V]): Option[MerkleTrieNode[K, V]] =
      diff.get(hash).flatMap {
        case (node, count) if count > 0 => Some(node)
        case _                          => None
      }

    def foreach(f: (MerkleHash[K, V], MerkleTrieNode[K, V]) => Unit): Unit =
      for (hash, (node, count)) <- diff if count > 0
      yield f(hash, node)
      ()

    def add(
        hash: MerkleHash[K, V],
        node: MerkleTrieNode[K, V],
    ): MerkleTrieStateDiff[K, V] =
      diff.get(hash).fold(diff + (hash -> (node, 1))) {
        case (node, -1)    => diff - hash
        case (node, count) => diff + (hash -> (node, count + 1))
      }

    def remove(
        hash: MerkleHash[K, V],
        node: MerkleTrieNode[K, V],
    ): MerkleTrieStateDiff[K, V] =
      diff.get(hash).fold(diff + (hash -> (node, -1))) {
        case (node, 1)     => diff - hash
        case (node, count) => diff + (hash -> (node, count - 1))
      }

    def toList: List[(MerkleHash[K, V], (MerkleTrieNode[K, V], Int))] =
      diff.toList
  end extension

