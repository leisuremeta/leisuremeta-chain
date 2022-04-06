package org.leisuremeta.lmchain.core
package codec.byte

import java.time.Instant

import cats.implicits._

import eu.timepit.refined.auto._
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import scodec.bits.ByteVector
import shapeless.{::, Generic, HList, HNil, Lazy}

import crypto.Hash
import datatype.{BigNat, UInt256Refined, UInt256Refine, UInt256Bytes, Utf8}
import UInt256Refine.UInt256RefineOps

trait ByteEncoder[A] {
  def encode(a: A): ByteVector

  def contramap[B](f: B => A): ByteEncoder[B] = { b => encode(f(b)) }
}

object ByteEncoder {

  def apply[A](implicit be: ByteEncoder[A]): ByteEncoder[A] = be

  object ops {
    implicit class ByteEncoderOps[A](val a: A) extends AnyVal {
      def toBytes(implicit be: ByteEncoder[A]): ByteVector = be.encode(a)
    }
  }

  implicit val hnilByteEncoder: ByteEncoder[HNil] = { _ => ByteVector.empty }

  implicit def hlistByteEncoder[H, T <: HList](implicit
      beh: Lazy[ByteEncoder[H]],
      bet: ByteEncoder[T],
  ): ByteEncoder[H :: T] = { case h :: t =>
    beh.value.encode(h) ++ bet.encode(t)
  }

  implicit def genericEncoder[A, B <: HList](implicit
      agen: Generic.Aux[A, B],
      beb: Lazy[ByteEncoder[B]],
  ): ByteEncoder[A] = beb.value contramap agen.to

  implicit val bignatEncoder: ByteEncoder[BigNat] = { bignat =>
    val bytes = ByteVector.view(bignat.toByteArray).dropWhile(_ === 0x00.toByte)
    if (bytes.isEmpty) ByteVector(0x00.toByte)
    else if (bignat <= 0x80) bytes
    else {
      val size = bytes.size
      if (size < (0xf8 - 0x80) + 1)
        ByteVector.fromByte((size + 0x80).toByte) ++ bytes
      else {
        val sizeBytes = ByteVector.fromLong(size).dropWhile(_ === 0x00.toByte)
        ByteVector.fromByte(
          (sizeBytes.size + 0xf8 - 1).toByte
        ) ++ sizeBytes ++ bytes
      }
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  private def nat(bigint: BigInt): BigNat = refineV[NonNegative](bigint) match {
    case Right(bignat) => bignat
    case Left(msg)     => throw new Exception(msg)
  }

  implicit def listEncoder[A: ByteEncoder]: ByteEncoder[List[A]] = { list =>
    list
      .map(ByteEncoder[A].encode)
      .foldLeft {
        ByteEncoder[BigNat].encode(nat(BigInt(list.size)))
      }(_ ++ _)
  }

  implicit def optionEncoder[A: ByteEncoder]: ByteEncoder[Option[A]] =
    listEncoder[A].contramap(_.toList)

  implicit def vectorEncoder[A: ByteEncoder]: ByteEncoder[Vector[A]] =
    listEncoder[A].contramap(_.toList)

  implicit def indexedSeqEncoder[A: ByteEncoder]: ByteEncoder[IndexedSeq[A]] =
    listEncoder[A].contramap(_.toList)

  private def sortedListEncoder[A: ByteEncoder]: ByteEncoder[List[A]] = {
    list =>
      list
        .map(ByteEncoder[A].encode)
        .sortBy(_.toHex)
        .foldLeft {
          ByteEncoder[BigNat].encode(nat(BigInt(list.size)))
        }(_ ++ _)
  }

  implicit val variableBytes: ByteEncoder[ByteVector] = { byteVector =>
    bignatEncoder
      .contramap((bytes: ByteVector) => nat(bytes.size))
      .encode(byteVector) ++ byteVector
  }

  implicit def setEncoder[A: ByteEncoder]: ByteEncoder[Set[A]] =
    sortedListEncoder[A].contramap(_.toList)

  implicit def mapEncoder[A: ByteEncoder, B: ByteEncoder]
      : ByteEncoder[Map[A, B]] = sortedListEncoder[(A, B)].contramap(_.toList)

  implicit def uint256RefineEncoder[A: UInt256RefineOps]
      : ByteEncoder[UInt256Refined[A]] = _.toBytes

  implicit val byteEncoder: ByteEncoder[Byte] = ByteVector.fromByte

  implicit val intEncoder: ByteEncoder[Int] = ByteVector.fromInt(_)

  implicit val longEncoder: ByteEncoder[Long] = ByteVector.fromLong(_)

  // support milliseconds only
  implicit val instantEncoder: ByteEncoder[Instant] =
    ByteVector fromLong _.toEpochMilli

  implicit val utf8Encoder: ByteEncoder[Utf8] = variableBytes.contramap(_.bytes)

  implicit def hashValueEncoder[A]: ByteEncoder[Hash.Value[A]] =
    ByteEncoder[UInt256Bytes].contramap { case (v: Hash.Value[A]) =>
      v
    }
}
