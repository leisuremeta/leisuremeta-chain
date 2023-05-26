/*
package io.leisuremeta.chain.lib.merkle

import GenericMerkleTrieNode.MerkleHash

opaque type GenericMerkleTrieStateDiff[K, V] =
  Map[MerkleHash[K, V], (GenericMerkleTrieNode[K, V], Int)]

object GenericMerkleTrieStateDiff:

  def apply[K, V](
      map: Map[MerkleHash[K, V], (GenericMerkleTrieNode[K, V], Int)],
  ): GenericMerkleTrieStateDiff[K, V] = map

  def empty[K, V]: GenericMerkleTrieStateDiff[K, V] = Map.empty

  extension [K, V](diff: GenericMerkleTrieStateDiff[K, V])
    def get(hash: MerkleHash[K, V]): Option[GenericMerkleTrieNode[K, V]] =
      diff.get(hash).flatMap {
        case (node, count) if count > 0 => Some(node)
        case _                          => None
      }

    def foreach(f: (MerkleHash[K, V], GenericMerkleTrieNode[K, V]) => Unit): Unit =
      for (hash, (node, count)) <- diff if count > 0
      yield f(hash, node)
      ()

    def add(
        hash: MerkleHash[K, V],
        node: GenericMerkleTrieNode[K, V],
    ): GenericMerkleTrieStateDiff[K, V] =
      diff.get(hash).fold(diff + (hash -> (node, 1))) {
        case (node, -1)    => diff - hash
        case (node, count) => diff + (hash -> (node, count + 1))
      }

    def remove(
        hash: MerkleHash[K, V],
        node: GenericMerkleTrieNode[K, V],
    ): GenericMerkleTrieStateDiff[K, V] =
      diff.get(hash).fold(diff + (hash -> (node, -1))) {
        case (node, 1)     => diff - hash
        case (node, count) => diff + (hash -> (node, count - 1))
      }

    def toList: List[(MerkleHash[K, V], (GenericMerkleTrieNode[K, V], Int))] =
      diff.toList

    def toMap: Map[MerkleHash[K, V], (GenericMerkleTrieNode[K, V], Int)] = diff
  end extension
*/
