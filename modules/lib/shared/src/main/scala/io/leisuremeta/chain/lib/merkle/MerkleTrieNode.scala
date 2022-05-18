package io.leisuremeta.chain.lib
package merkle

import scala.compiletime.constValue

import cats.Eq
import cats.syntax.eq.*
import eu.timepit.refined.refineV
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.*
import eu.timepit.refined.boolean.And
import eu.timepit.refined.collection.Size
import eu.timepit.refined.generic.Equal
import eu.timepit.refined.numeric.Divisible
import scodec.bits.{BitVector, ByteVector}

import codec.byte.{ByteDecoder, ByteEncoder, DecodeResult}
import datatype.{BigNat, UInt256}
import crypto.Hash
import failure.DecodingFailure
import util.refined.bitVector.given

sealed trait MerkleTrieNode[K, V]:

  def prefix: MerkleTrieNode.Prefix

  def getChildren: Option[MerkleTrieNode.Children[K, V]] = this match
    case MerkleTrieNode.Leaf(_, _)                     => None
    case MerkleTrieNode.Branch(_, children)            => Some(children)
    case MerkleTrieNode.BranchWithData(_, children, _) => Some(children)

  def getValue: Option[ByteVector] = this match
    case MerkleTrieNode.Leaf(_, value)              => Some(value)
    case MerkleTrieNode.Branch(_, _)                => None
    case MerkleTrieNode.BranchWithData(_, _, value) => Some(value)

  def setPrefix(prefix: MerkleTrieNode.Prefix): MerkleTrieNode[K, V] =
    this match
      case MerkleTrieNode.Leaf(_, value) => MerkleTrieNode.Leaf(prefix, value)
      case MerkleTrieNode.Branch(_, children) =>
        MerkleTrieNode.Branch(prefix, children)
      case MerkleTrieNode.BranchWithData(_, key, value) =>
        MerkleTrieNode.BranchWithData(prefix, key, value)

  def setChildren(
      children: MerkleTrieNode.Children[K, V],
  ): MerkleTrieNode[K, V] = this match
    case MerkleTrieNode.Leaf(prefix, value) =>
      MerkleTrieNode.BranchWithData(prefix, children, value)
    case MerkleTrieNode.Branch(prefix, _) =>
      MerkleTrieNode.Branch(prefix, children)
    case MerkleTrieNode.BranchWithData(prefix, _, value) =>
      MerkleTrieNode.BranchWithData(prefix, children, value)

  def setValue(value: ByteVector): MerkleTrieNode[K, V] = this match
    case MerkleTrieNode.Leaf(prefix, _) => MerkleTrieNode.Leaf(prefix, value)
    case MerkleTrieNode.Branch(prefix, children) =>
      MerkleTrieNode.BranchWithData(prefix, children, value)
    case MerkleTrieNode.BranchWithData(prefix, children, _) =>
      MerkleTrieNode.BranchWithData(prefix, children, value)

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  override def toString: String =
    val childrenString = getChildren.fold("[]") { (childrenRefined) =>
      val ss = for i <- 0 until 16 yield f"${i}%x: ${childrenRefined.value(i)}"
      ss.mkString("[", ",", "]")
    }
    s"MerkleTrieNode(${prefix.value.toHex}, $childrenString, $getValue)"

object MerkleTrieNode:

  final case class Leaf[K, V](prefix: Prefix, value: ByteVector)
      extends MerkleTrieNode[K, V]
  final case class Branch[K, V](prefix: Prefix, children: Children[K, V])
      extends MerkleTrieNode[K, V]
  final case class BranchWithData[K, V](
      prefix: Prefix,
      children: Children[K, V],
      value: ByteVector,
  ) extends MerkleTrieNode[K, V]

  def leaf[K, V](prefix: Prefix, value: ByteVector): MerkleTrieNode[K, V] =
    Leaf(prefix, value)

  def branch[K, V](
      prefix: Prefix,
      children: Children[K, V],
  ): MerkleTrieNode[K, V] = Branch(prefix, children)

  def branchWithData[K, V](
      prefix: Prefix,
      children: Children[K, V],
      value: ByteVector,
  ): MerkleTrieNode[K, V] = BranchWithData(prefix, children, value)

  type MerkleHash[K, V] = Hash.Value[MerkleTrieNode[K, V]]
  type MerkleRoot[K, V] = MerkleHash[K, V]

  type Prefix = BitVector Refined PrefixCondition

  type PrefixCondition = Size[Divisible[4L]]

  type Children[K, V] = Vector[Option[MerkleHash[K, V]]] Refined
    ChildrenCondition

  type ChildrenCondition = Size[Equal[16]]

  extension [K, V](c: Children[K, V])
    @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
    def updated(i: Int, v: Option[MerkleHash[K, V]]): Children[K, V] =
      refineV[ChildrenCondition](c.value.updated(i, v)).toOption.get

  object Children:
    @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
    def empty[K, V]: Children[K, V] = refineV[ChildrenCondition](
      Vector.fill(16)(Option.empty[MerkleHash[K, V]]),
    ).toOption.get

  given [K, V]: Eq[MerkleTrieNode[K, V]] = Eq.fromUniversalEquals

  given merkleTrieNodeEncoder[K, V]: ByteEncoder[MerkleTrieNode[K, V]] = node =>
    val encodePrefix: ByteVector =
      val prefixNibbleSize: Long = node.prefix.value.size / 4
      ByteEncoder[BigNat].encode(
        BigNat.unsafeFromBigInt(BigInt(prefixNibbleSize)),
      ) ++ node.prefix.bytes

    def encodeChildren(children: MerkleTrieNode.Children[K, V]): ByteVector =
      val existanceBytes = BitVector.bits(children.value.map(_.nonEmpty)).bytes
      children.value
        .flatMap(_.toList)
        .foldLeft(existanceBytes)(_ ++ _.toUInt256Bytes)

    def encodeValue(value: ByteVector): ByteVector =
      ByteEncoder[BigNat].encode(
        BigNat.unsafeFromBigInt(BigInt(value.size)),
      ) ++ value
    val encoded = node match
      case Leaf(_, value) =>
        ByteVector.fromByte(1) ++ encodePrefix ++ encodeValue(value)
      case Branch(_, children) =>
        ByteVector.fromByte(2) ++ encodePrefix ++ encodeChildren(children)
      case BranchWithData(_, children, value) =>
        ByteVector.fromByte(3) ++ encodePrefix ++ encodeChildren(
          children,
        ) ++ encodeValue(value)
    encoded

  given merkleTrieNodeDecoder[K, V]: ByteDecoder[MerkleTrieNode[K, V]] =
    val prefixDecoder: ByteDecoder[MerkleTrieNode.Prefix] =
      val unrefinedPrefixDecoder = for
        prefixNibbleSize <- ByteDecoder[BigNat]
        prefixNibbleSizeLong = prefixNibbleSize.toBigInt.toLong
        prefix <- ByteDecoder.fromFixedSizeBytes(
          (prefixNibbleSizeLong + 1) / 2,
        ) { prefixBytes =>
          val padSize = prefixNibbleSizeLong * 4 - prefixBytes.size * 8
          val prefixBits =
            if padSize > 0 then prefixBytes.bits.padLeft(padSize)
            else prefixBytes.bits
          prefixBits.take(prefixNibbleSizeLong * 4)
        }
      yield prefix

      unrefinedPrefixDecoder.emap(prefix =>
        refineV[PrefixCondition](prefix).left.map(DecodingFailure(_)),
      )

    val childrenDecoder: ByteDecoder[MerkleTrieNode.Children[K, V]] =
      ByteDecoder
        .fromFixedSizeBytes(2)(_.bits)
        .flatMap { (existanceBits) => (bytes) =>
          type LoopType = Either[DecodingFailure, DecodeResult[
            Vector[Option[MerkleHash[K, V]]],
          ]]
          @annotation.tailrec
          @SuppressWarnings(Array("org.wartremover.warts.Nothing"))
          def loop(
              bits: BitVector,
              bytes: ByteVector,
              acc: List[Option[MerkleHash[K, V]]],
          ): LoopType = bits.headOption match
            case None =>
              Right(DecodeResult(acc.reverse.toVector, bytes))
            case Some(false) =>
              loop(bits.tail, bytes, None :: acc)
            case Some(true) =>
              val (hashBytes, rest) = bytes.splitAt(32)
              UInt256.from(hashBytes) match
                case Left(err) => Left(DecodingFailure(err.msg))
                case Right(hash) =>
                  loop(
                    bits.tail,
                    rest,
                    Some(Hash.Value[MerkleTrieNode[K, V]](hash)) :: acc,
                  )
          end loop
          loop(existanceBits, bytes, Nil)
        }
        .emap[Children[K, V]] { vector =>
          refineV[ChildrenCondition](vector).left.map(DecodingFailure(_))
        }

    val valueDecoder: ByteDecoder[ByteVector] = ByteDecoder[BigNat].flatMap {
      (size) =>
        ByteDecoder.fromFixedSizeBytes(size.toBigInt.toLong)(identity)
    }

    ByteDecoder.byteDecoder
      .emap { b =>
        Either.cond(1 <= b && b <= 3, b, DecodingFailure(s"wrong range: $b"))
      }
      .flatMap {
        case 1 =>
          for
            prefix <- prefixDecoder
            value  <- valueDecoder
          yield Leaf(prefix, value)
        case 2 =>
          for
            prefix   <- prefixDecoder
            children <- childrenDecoder
          yield Branch(prefix, children)
        case 3 =>
          for
            prefix   <- prefixDecoder
            children <- childrenDecoder
            value    <- valueDecoder
          yield BranchWithData(prefix, children, value)
      }

  given merkleTrieNodeHash[K, V]: Hash[MerkleTrieNode[K, V]] = Hash.build
