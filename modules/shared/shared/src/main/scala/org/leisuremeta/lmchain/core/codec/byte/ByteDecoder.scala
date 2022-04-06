package org.leisuremeta.lmchain.core
package codec.byte

import java.time.Instant

import scala.reflect._

import cats.implicits._

import org.leisuremeta.lmchain.core.crypto.Hash
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import scodec.bits.{BitVector, ByteVector}
import shapeless.ops.nat.ToInt
import shapeless.syntax.sized._
import shapeless.{::, Generic, HList, HNil, Lazy, Nat, Sized, tag}

import datatype.{BigNat, UInt256BigInt, UInt256Bytes, UInt256Refine, Utf8}
import failure.DecodingFailure

trait ByteDecoder[A] {
  def decode(bytes: ByteVector): Either[DecodingFailure, DecodeResult[A]]

  def map[B](f: A => B): ByteDecoder[B] = { bytes =>
    decode(bytes).map { case DecodeResult(a, remainder) =>
      DecodeResult(f(a), remainder)
    }
  }

  def emap[B](f: A => Either[DecodingFailure, B]): ByteDecoder[B] = { bytes =>
    for {
      decoded   <- decode(bytes)
      converted <- f(decoded.value)
    } yield DecodeResult(converted, decoded.remainder)
  }

  def flatMap[B](f: A => ByteDecoder[B]): ByteDecoder[B] = { bytes =>
    decode(bytes).flatMap { case DecodeResult(a, remainder) =>
      f(a).decode(remainder)
    }
  }
}

final case class DecodeResult[+A](value: A, remainder: ByteVector)

object ByteDecoder {

  def apply[A](implicit bd: ByteDecoder[A]): ByteDecoder[A] = bd

  implicit val hnilByteDecoder: ByteDecoder[HNil] = { bytes =>
    Right[DecodingFailure, DecodeResult[HNil]](DecodeResult(HNil, bytes))
  }

  implicit def hlistByteDecoder[H, T <: HList](implicit
      bdh: Lazy[ByteDecoder[H]],
      bdt: ByteDecoder[T],
  ): ByteDecoder[H :: T] = { bytes =>
    for {
      decodedH <- bdh.value.decode(bytes)
      decodedT <- bdt.decode(decodedH.remainder)
    } yield DecodeResult(decodedH.value :: decodedT.value, decodedT.remainder)
  }

  implicit def genericDecoder[A, B <: HList](implicit
      agen: Generic.Aux[A, B],
      bdb: Lazy[ByteDecoder[B]],
  ): ByteDecoder[A] = bdb.value map agen.from

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  private def nat(bigint: BigInt): BigNat = refineV[NonNegative](bigint) match {
    case Right(bignat) => bignat
    case Left(msg)     => throw new Exception(msg)
  }

  implicit val bignatDecoder: ByteDecoder[BigNat] = { bytes =>
    Either.cond(bytes.nonEmpty, bytes, DecodingFailure("Empty bytes")).flatMap {
      nonEmptyBytes =>
        val head: Int        = nonEmptyBytes.head & 0xff
        val tail: ByteVector = nonEmptyBytes.tail
        if (head <= 0x80)
          Right[DecodingFailure, DecodeResult[BigNat]](
            DecodeResult(nat(BigInt(head)), tail)
          )
        else if (head <= 0xf8) {
          val size = head - 0x80
          if (tail.size < size)
            Left[DecodingFailure, DecodeResult[BigNat]](
              DecodingFailure(
                s"required byte size $size, but $tail"
              )
            )
          else {
            val (front, back) = tail.splitAt(size.toLong)
            Right[DecodingFailure, DecodeResult[BigNat]](
              DecodeResult(nat(BigInt(1, front.toArray)), back)
            )
          }
        } else {
          val sizeOfNumber = head - 0xf8 + 1
          if (tail.size < sizeOfNumber)
            Left[DecodingFailure, DecodeResult[BigNat]](
              DecodingFailure(
                s"required byte size $sizeOfNumber, but $tail"
              )
            )
          else {
            val (sizeBytes, data) = tail.splitAt(sizeOfNumber.toLong)
            val size              = BigInt(1, sizeBytes.toArray).toLong

            if (data.size < size)
              Left[DecodingFailure, DecodeResult[BigNat]](
                DecodingFailure(
                  s"required byte size $size, but $data"
                )
              )
            else {
              val (front, back) = data.splitAt(size)
              Right[DecodingFailure, DecodeResult[BigNat]](
                DecodeResult(nat(BigInt(1, front.toArray)), back)
              )
            }
          }
        }
    }
  }

  def sizedListDecoder[A: ByteDecoder](size: BigNat): ByteDecoder[List[A]] = {
    bytes =>
      @annotation.tailrec
      def loop(
          bytes: ByteVector,
          count: BigInt,
          acc: List[A],
      ): Either[DecodingFailure, DecodeResult[List[A]]] = {
        if (count === BigInt(0)) {
          Right[DecodingFailure, DecodeResult[List[A]]](
            DecodeResult(acc.reverse, bytes)
          )
        } else
          ByteDecoder[A].decode(bytes) match {
            case Left(failure) =>
              Left[DecodingFailure, DecodeResult[List[A]]](failure)
            case Right(DecodeResult(value, remainder)) =>
              loop(remainder, nat(count - 1), value :: acc)
          }
      }
      loop(bytes, size, Nil)
  }

  implicit def listDecoder[A: ByteDecoder]: ByteDecoder[List[A]] =
    ByteDecoder[BigNat] flatMap sizedListDecoder[A]

  implicit def optionDecoder[A: ByteDecoder]: ByteDecoder[Option[A]] =
    listDecoder[A].map(_.headOption)

  implicit def vectorDecoder[A: ByteDecoder]: ByteDecoder[Vector[A]] =
    listDecoder[A].map(_.toVector)

  implicit def indexedSeqEncoder[A: ByteDecoder]: ByteDecoder[IndexedSeq[A]] =
    listDecoder[A].map(_.toIndexedSeq)

  implicit def setDecoder[A: ByteDecoder]: ByteDecoder[Set[A]] =
    ByteDecoder[List[A]].map(_.toSet)

  implicit def mapDecoder[A: ByteDecoder, B: ByteDecoder]
      : ByteDecoder[Map[A, B]] = ByteDecoder[List[(A, B)]].map(_.toMap)

  def fromFixedSizeBytes[T: ClassTag](
      size: Long
  )(f: ByteVector => T): ByteDecoder[T] = { bytes =>
    Either.cond(
      bytes.size >= size,
      bytes splitAt size match {
        case (front, back) => DecodeResult(f(front), back)
      },
      DecodingFailure(
        s"Too shord bytes to decode ${classTag[T]}; required $size bytes, but receiced ${bytes.size} bytes: $bytes"
      ),
    )
  }

  implicit val variableBytes: ByteDecoder[ByteVector] =
    bignatDecoder.flatMap(bignat => fromFixedSizeBytes(bignat.toLong)(identity))

  @SuppressWarnings(
    Array(
      "org.wartremover.warts.ImplicitParameter",
      "org.wartremover.warts.OptionPartial",
    )
  )
  def fixedSizedVectorDecoder[A: ByteDecoder](
      size: Nat
  )(implicit toInt: ToInt[size.N]): ByteDecoder[Vector[A] Sized size.N] =
    sizedListDecoder[A](refineV[NonNegative](BigInt(toInt())).toOption.get)
      .map(_.toVector.sized(size).get)

  @SuppressWarnings(
    Array(
      "org.wartremover.warts.ImplicitParameter",
      "org.wartremover.warts.TraversableOps",
    )
  )
  def fixedSizedOptionalVectorDecoder[A: ByteDecoder](size: Nat)(implicit
      toInt: ToInt[size.N]
  ): ByteDecoder[Vector[Option[A]] Sized size.N] =
    fromFixedSizeBytes(((toInt() + 7) / 8).toLong)(_.bits).flatMap { bits =>
      sizedListDecoder[A](nat(BigInt(bits.populationCount))).emap { list =>
        @annotation.tailrec
        def loop(
            bits: BitVector,
            list: List[A],
            acc: List[Option[A]],
        ): Either[DecodingFailure, List[Option[A]]] = {
          if (bits.isEmpty) Right(acc.reverse)
          else
            (list match {
              case _ if !bits.head => loop(bits.tail, list, None :: acc)
              case Nil =>
                Left(DecodingFailure(s"Not enough bytes: $bits $list $acc"))
              case _ => loop(bits.tail, list.tail, Some(list.head) :: acc)
            })
        }
        loop(bits, list, List.empty).map(_.toVector.ensureSized[size.N])
      }
    }

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  implicit val uint256bigintDecoder: ByteDecoder[UInt256BigInt] =
    fromFixedSizeBytes(32) { bytes =>
      UInt256Refine.from(BigInt(1, bytes.toArray)).toOption.get
    }

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  implicit val uint256bytesDecoder: ByteDecoder[UInt256Bytes] =
    fromFixedSizeBytes(32) { UInt256Refine.from(_).toOption.get }

  implicit val byteDecoder: ByteDecoder[Byte] =
    fromFixedSizeBytes(1)(_.toByte())

  implicit val utf8Decoder: ByteDecoder[Utf8] = bignatDecoder.flatMap { size =>
    fromFixedSizeBytes(size.value.toLong)(Utf8.decode).emap {
      _.left.map(DecodingFailure(_))
    }
  }

  implicit val intDecoder: ByteDecoder[Int] =
    fromFixedSizeBytes(4)(_.toInt())

  implicit val longDecoder: ByteDecoder[Long] =
    fromFixedSizeBytes(8)(_.toLong())

  implicit val instantDecoder: ByteDecoder[Instant] =
    ByteDecoder[Long] map Instant.ofEpochMilli

  implicit def hashvalueDecoder[A]: ByteDecoder[Hash.Value[A]] =
    ByteDecoder[UInt256Bytes].map(tag[A][UInt256Bytes](_))
}
