package org.leisuremeta.lmchain.core
package crypto

import cats.Eq
import eu.timepit.refined.refineV
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.boolean.And
import eu.timepit.refined.collection.Size
import eu.timepit.refined.numeric.{Divisible, Interval}
import scodec.bits.{BitVector, ByteVector}
import shapeless.Sized
import shapeless.nat._16

import codec.byte.{ByteDecoder, ByteEncoder}
import failure.DecodingFailure
import util.refined.bitVector._

sealed trait MerkleTrieNode[K, V] {
  def prefix: MerkleTrieNode.Prefix
}

object MerkleTrieNode {
  final case class Branch[K, V](prefix: Prefix, children: Children[K, V])
      extends MerkleTrieNode[K, V]
  final case class Leaf[K, V](prefix: Prefix, value: ByteVector)
      extends MerkleTrieNode[K, V]

  def branch[K, V](
      prefix: Prefix,
      children: Children[K, V],
  ): MerkleTrieNode[K, V] =
    Branch(prefix, children)

  def leaf[K, V](prefix: Prefix, value: ByteVector): MerkleTrieNode[K, V] =
    Leaf(prefix, value)

  type MerkleHash[K, V] = Hash.Value[MerkleTrieNode[K, V]]
  type MerkleRoot[K, V] = MerkleHash[K, V]

  type Children[K, V] = Vector[Option[MerkleHash[K, V]]] Sized _16

  type PrefixCondition = Size[Interval.Closed[0L, 508L] And Divisible[4L]]

  type Prefix = BitVector Refined PrefixCondition

  implicit def merkleRootCirceDecoder[K, V]: io.circe.Decoder[MerkleRoot[K, V]] =
    codec.circe.hashValueDecoder[MerkleTrieNode[K, V]]

  implicit def merkleRootCirceEncoder[K, V]: io.circe.Encoder[MerkleRoot[K, V]] =
    codec.circe.hashValueEncoder[MerkleTrieNode[K, V]]

  implicit def MerkleTrieNodeEqInstance[K, V]: Eq[MerkleTrieNode[K, V]] =
    Eq.fromUniversalEquals

  implicit def merkleTrieNodeEncoder[K, V]
      : ByteEncoder[MerkleTrieNode[K, V]] = { node =>
    val prefixNibbleSize = node.prefix.size / 4
    val (int, bytes) = node match {
      case Branch(prefix, children) =>
        val existanceBytes =
          BitVector.bits(children.unsized.map(_.nonEmpty)).bytes
        val concat: ByteVector =
          children.flatMap(_.toList).foldLeft(existanceBytes)(_ ++ _)
        (prefixNibbleSize, prefix.bytes ++ concat)
      case Leaf(prefix, value) =>
        (
          (1 << 7) + prefixNibbleSize,
          prefix.bytes ++ ByteEncoder.variableBytes.encode(value),
        )
    }
    ByteVector.fromByte(int.toByte) ++ bytes
  }

  implicit def merkleTrieNodeDecoder[K, V]: ByteDecoder[MerkleTrieNode[K, V]] =
    for {
      byte <- ByteDecoder.byteDecoder
      uint8          = byte & 0xff
      numberOfNibble = (uint8 % (1 << 7)).toLong
      prefixRefined <- ByteDecoder
        .fromFixedSizeBytes((numberOfNibble + 1) / 2) {
          _.bits.take(numberOfNibble * 4)
        }
        .emap { prefix =>
          refineV[PrefixCondition](prefix).left.map(DecodingFailure(_))
        }
      decoder <- ((uint8 >> 7) match {
        case 0 =>
          ByteDecoder
            .fixedSizedOptionalVectorDecoder[MerkleHash[K, V]](_16)
            .map(Branch[K, V](prefixRefined, _))
        case 1 =>
          ByteDecoder.variableBytes.map(Leaf[K, V](prefixRefined, _))
      })
    } yield decoder
}
