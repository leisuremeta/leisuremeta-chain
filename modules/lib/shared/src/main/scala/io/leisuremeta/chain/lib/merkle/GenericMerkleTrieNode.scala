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

sealed trait GenericMerkleTrieNode[K, V]:

  def prefix: GenericMerkleTrieNode.Prefix

  def getChildren: Option[GenericMerkleTrieNode.Children[K, V]] = this match
    case GenericMerkleTrieNode.Leaf(_, _)                     => None
    case GenericMerkleTrieNode.Branch(_, children)            => Some(children)
    case GenericMerkleTrieNode.BranchWithData(_, children, _) => Some(children)

  def getValue: Option[ByteVector] = this match
    case GenericMerkleTrieNode.Leaf(_, value)              => Some(value)
    case GenericMerkleTrieNode.Branch(_, _)                => None
    case GenericMerkleTrieNode.BranchWithData(_, _, value) => Some(value)

  def setPrefix(prefix: GenericMerkleTrieNode.Prefix): GenericMerkleTrieNode[K, V] =
    this match
      case GenericMerkleTrieNode.Leaf(_, value) => GenericMerkleTrieNode.Leaf(prefix, value)
      case GenericMerkleTrieNode.Branch(_, children) =>
        GenericMerkleTrieNode.Branch(prefix, children)
      case GenericMerkleTrieNode.BranchWithData(_, key, value) =>
        GenericMerkleTrieNode.BranchWithData(prefix, key, value)

  def setChildren(
      children: GenericMerkleTrieNode.Children[K, V],
  ): GenericMerkleTrieNode[K, V] = this match
    case GenericMerkleTrieNode.Leaf(prefix, value) =>
      GenericMerkleTrieNode.BranchWithData(prefix, children, value)
    case GenericMerkleTrieNode.Branch(prefix, _) =>
      GenericMerkleTrieNode.Branch(prefix, children)
    case GenericMerkleTrieNode.BranchWithData(prefix, _, value) =>
      GenericMerkleTrieNode.BranchWithData(prefix, children, value)

  def setValue(value: ByteVector): GenericMerkleTrieNode[K, V] = this match
    case GenericMerkleTrieNode.Leaf(prefix, _) => GenericMerkleTrieNode.Leaf(prefix, value)
    case GenericMerkleTrieNode.Branch(prefix, children) =>
      GenericMerkleTrieNode.BranchWithData(prefix, children, value)
    case GenericMerkleTrieNode.BranchWithData(prefix, children, _) =>
      GenericMerkleTrieNode.BranchWithData(prefix, children, value)

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  override def toString: String =
    val childrenString = getChildren.fold("[]") { (childrenRefined) =>
      val ss = for i <- 0 until 16 yield f"${i}%x: ${childrenRefined.value(i)}"
      ss.mkString("[", ",", "]")
    }
    s"GenericMerkleTrieNode(${prefix.value.toHex}, $childrenString, $getValue)"

object GenericMerkleTrieNode:

  final case class Leaf[K, V](prefix: Prefix, value: ByteVector)
      extends GenericMerkleTrieNode[K, V]
  final case class Branch[K, V](prefix: Prefix, children: Children[K, V])
      extends GenericMerkleTrieNode[K, V]
  final case class BranchWithData[K, V](
      prefix: Prefix,
      children: Children[K, V],
      value: ByteVector,
  ) extends GenericMerkleTrieNode[K, V]

  def leaf[K, V](prefix: Prefix, value: ByteVector): GenericMerkleTrieNode[K, V] =
    Leaf(prefix, value)

  def branch[K, V](
      prefix: Prefix,
      children: Children[K, V],
  ): GenericMerkleTrieNode[K, V] = Branch(prefix, children)

  def branchWithData[K, V](
      prefix: Prefix,
      children: Children[K, V],
      value: ByteVector,
  ): GenericMerkleTrieNode[K, V] = BranchWithData(prefix, children, value)

  type MerkleHash[K, V] = Hash.Value[GenericMerkleTrieNode[K, V]]
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

  given [K, V]: Eq[GenericMerkleTrieNode[K, V]] = Eq.fromUniversalEquals

  given GenericMerkleTrieNodeEncoder[K, V]: ByteEncoder[GenericMerkleTrieNode[K, V]] = node =>
    val encodePrefix: ByteVector =
      val prefixNibbleSize: Long = node.prefix.value.size / 4
      ByteEncoder[BigNat].encode(
        BigNat.unsafeFromBigInt(BigInt(prefixNibbleSize)),
      ) ++ node.prefix.bytes

    def encodeChildren(children: GenericMerkleTrieNode.Children[K, V]): ByteVector =
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

  given GenericMerkleTrieNodeDecoder[K, V]: ByteDecoder[GenericMerkleTrieNode[K, V]] =
    val prefixDecoder: ByteDecoder[GenericMerkleTrieNode.Prefix] =
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

    val childrenDecoder: ByteDecoder[GenericMerkleTrieNode.Children[K, V]] =
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
                    Some(Hash.Value[GenericMerkleTrieNode[K, V]](hash)) :: acc,
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

  given GenericMerkleTrieNodeHash[K, V]: Hash[GenericMerkleTrieNode[K, V]] = Hash.build
