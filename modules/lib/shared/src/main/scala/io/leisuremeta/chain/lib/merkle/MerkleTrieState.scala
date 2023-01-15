package io.leisuremeta.chain.lib.merkle

import MerkleTrieNode.MerkleRoot

final case class MerkleTrieState(
    root: Option[MerkleRoot],
    base: Option[MerkleRoot],
    diff: MerkleTrieStateDiff,
):
  @SuppressWarnings(
    Array("org.wartremover.warts.Equals", "org.wartremover.warts.Nothing"),
  )
  def rebase(
      that: MerkleTrieState,
  ): Either[String, MerkleTrieState] =
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
  def empty: MerkleTrieState = MerkleTrieState(
    None,
    None,
    MerkleTrieStateDiff.empty,
  )
  def fromRoot(root: MerkleRoot): MerkleTrieState =
    MerkleTrieState(
      root = Some(root),
      base = Some(root),
      diff = MerkleTrieStateDiff.empty,
    )
