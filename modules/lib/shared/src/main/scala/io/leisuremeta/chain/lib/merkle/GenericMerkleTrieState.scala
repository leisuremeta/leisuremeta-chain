/*
package io.leisuremeta.chain.lib.merkle

import GenericMerkleTrieNode.MerkleRoot

final case class GenericMerkleTrieState[K, V](
    root: Option[MerkleRoot[K, V]],
    base: Option[MerkleRoot[K, V]],
    diff: GenericMerkleTrieStateDiff[K, V],
):
  @SuppressWarnings(
    Array("org.wartremover.warts.Equals", "org.wartremover.warts.Nothing"),
  )
  def rebase(
      that: GenericMerkleTrieState[K, V],
  ): Either[String, GenericMerkleTrieState[K, V]] =
    if that.base != base then Left(s"Different base")
    else
      val thisMap = this.diff.toMap
      val thatMap = that.diff.toMap

      val map1 = thisMap.map { case (k, (v, count)) =>
        val (_, thatCount) = thatMap.getOrElse(k, (v, 0))
        (k, (v, count + thatCount))
      }.toMap

      Right(
        this.copy(
          base = that.root,
          diff = GenericMerkleTrieStateDiff(map1),
        ),
      )

object GenericMerkleTrieState:
  def empty[K, V]: GenericMerkleTrieState[K, V] = GenericMerkleTrieState(
    None,
    None,
    GenericMerkleTrieStateDiff.empty[K, V],
  )
  def fromRoot[K, V](root: MerkleRoot[K, V]): GenericMerkleTrieState[K, V] =
    GenericMerkleTrieState[K, V](
      root = Some(root),
      base = Some(root),
      diff = GenericMerkleTrieStateDiff.empty[K, V],
    )
*/
