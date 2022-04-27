package io.leisuremeta.chain.lib
package codec.byte

import scala.deriving.Mirror
import scala.reflect.{ClassTag, classTag}

import scodec.bits.ByteVector

import datatype.{UInt256, UInt256BigInt, UInt256Bytes}
import failure.DecodingFailure

trait ByteDecoder[A]:
  def decode(bytes: ByteVector): Either[DecodingFailure, DecodeResult[A]]

  def map[B](f: A => B): ByteDecoder[B] = bytes =>
    decode(bytes).map { case DecodeResult(a, remainder) =>
      DecodeResult(f(a), remainder)
    }

  def emap[B](f: A => Either[DecodingFailure, B]): ByteDecoder[B] = bytes =>
    for
      decoded   <- decode(bytes)
      converted <- f(decoded.value)
    yield DecodeResult(converted, decoded.remainder)

  def flatMap[B](f: A => ByteDecoder[B]): ByteDecoder[B] = bytes =>
    decode(bytes).flatMap { case DecodeResult(a, remainder) =>
      f(a).decode(remainder)
    }

final case class DecodeResult[+A](value: A, remainder: ByteVector)

object ByteDecoder:
  def apply[A: ByteDecoder]: ByteDecoder[A] = summon

  given ByteDecoder[EmptyTuple] = bytes =>
    Right[DecodingFailure, DecodeResult[EmptyTuple]](
      DecodeResult(EmptyTuple, bytes),
    )

  given [H, T <: Tuple](using
      bdh: ByteDecoder[H],
      bdt: ByteDecoder[T],
  ): ByteDecoder[H *: T] = bytes =>
    for
      decodedH <- bdh.decode(bytes)
      decodedT <- bdt.decode(decodedH.remainder)
    yield DecodeResult(decodedH.value *: decodedT.value, decodedT.remainder)

  given genericDecoder[P <: Product](using
      m: Mirror.ProductOf[P],
      bd: ByteDecoder[m.MirroredElemTypes],
  ): ByteDecoder[P] = bd map m.fromProduct

  def fromFixedSizeBytes[T: ClassTag](
      size: Long,
  )(f: ByteVector => T): ByteDecoder[T] = bytes =>
    Either.cond(
      bytes.size >= size,
      bytes splitAt size match
        case (front, back) => DecodeResult(f(front), back)
      ,
      DecodingFailure(
        s"Too shord bytes to decode ${classTag[T]}; required $size bytes, but receiced ${bytes.size} bytes: $bytes",
      ),
    )

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  given ByteDecoder[UInt256BigInt] = fromFixedSizeBytes(32) { bytes =>
    UInt256.from(BigInt(1, bytes.toArray)).toOption.get
  }

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  given ByteDecoder[UInt256Bytes] = fromFixedSizeBytes(32) {
    UInt256.from(_).toOption.get
  }

  given byteDecoder: ByteDecoder[Byte] = fromFixedSizeBytes(1)(_.toByte())
