package io.leisuremeta.chain.lib.merkle

import MerkleTrieNode.MerkleRoot

final case class MerkleTrieState[K, V](
    root: Option[MerkleRoot[K, V]],
    base: Option[MerkleRoot[K, V]],
    diff: MerkleTrieStateDiff[K, V],
):
  @SuppressWarnings(
    Array("org.wartremover.warts.Equals", "org.wartremover.warts.Nothing"),
  )
  def rebase(
      that: MerkleTrieState[K, V],
  ): Either[String, MerkleTrieState[K, V]] =
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
          diff = MerkleTrieStateDiff(map1),
        ),
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
