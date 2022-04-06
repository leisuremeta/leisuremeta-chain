package org.leisuremeta.lmchain.core
package codec.circe

import java.time.{Instant, OffsetDateTime, ZoneOffset}

import scala.util.Try

import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import scodec.bits.{BitVector, ByteVector}
import shapeless.tag.@@
import shapeless.{Lazy, tag}

import crypto.Hash
import datatype.{UInt256BigInt, UInt256Bytes, UInt256Refine}
trait CirceCodec {

  implicit val circeUInt256BigIntDecoder: Decoder[UInt256BigInt] =
    Decoder.decodeString.emap { (str: String) =>
      UInt256Refine.from[BigInt](BigInt(str, 16))
    }

  implicit val circeUInt256BigIntEncoder: Encoder[UInt256BigInt] =
    Encoder.encodeString.contramap(_.toString(16))

  implicit val circeUInt256BytesDecoder: Decoder[UInt256Bytes] =
    Decoder.decodeString.emap((str: String) =>
      for {
        bytes   <- ByteVector.fromHexDescriptive(str)
        refined <- UInt256Refine.from(bytes)
      } yield refined
    )

  implicit val circeUInt256BytesEncoder: Encoder[UInt256Bytes] =
    Encoder.encodeString.contramap(_.toHex)

  implicit val circeBitVectorDecoder: Decoder[BitVector] =
    Decoder.decodeString.emap { (str: String) =>
      BitVector.fromBase64(str).toRight(s"Base64 decoding failure: $str")
    }

  def taggedDecoder[A, B](implicit da: Lazy[Decoder[A]]): Decoder[A @@ B] =
    da.value.map(tag[B][A](_))
  def taggedEncoder[A: Encoder, B]: Encoder[A @@ B] =
    Encoder[A].contramap(_.asInstanceOf[A])

  def hashValueDecoder[A]: Decoder[Hash.Value[A]] = taggedDecoder[UInt256Bytes, A]
  def hashValueEncoder[A]: Encoder[Hash.Value[A]] = taggedEncoder[UInt256Bytes, A]

  implicit def circeBitVectorEncoder[A <: BitVector]: Encoder[A] =
    Encoder.encodeString.contramap[A](_.toBase64)

  implicit val circeByteVectorDecoder: Decoder[ByteVector] =
    Decoder.decodeString.emap { (str: String) =>
      ByteVector.fromBase64(str).toRight(s"Base64 decoding failure: $str")
    }

  implicit val circeByteVectorEncoder: Encoder[ByteVector] =
    Encoder.encodeString.contramap[ByteVector](_.toBase64)

  implicit val circeInstantDecoder: Decoder[Instant] =
    Decoder.decodeString.emap { (str: String) =>
      Try(OffsetDateTime.parse(str)).toEither
        .map(_.toInstant())
        .left
        .map(_.getMessage)
    }

  implicit val circeInstantEncoder: Encoder[Instant] =
    Encoder.encodeString.contramap {
      _.atOffset(ZoneOffset.UTC).toString()
    }

  implicit val circeUInt256BytesKeyDecoder: KeyDecoder[UInt256Bytes] =
    KeyDecoder.instance((str: String) =>
      for {
        bytes   <- ByteVector.fromBase64(str)
        refined <- UInt256Refine.from(bytes).toOption
      } yield refined
    )

  implicit val circeUInt256BytesKeyEncoder: KeyEncoder[UInt256Bytes] =
    KeyEncoder.instance(_.toBytes.toBase64)

}
