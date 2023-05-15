package io.leisuremeta.chain.lib
package datatype

import java.nio.charset.{CharacterCodingException, StandardCharsets}
import scala.util.Try

import cats.Eq

import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import scodec.bits.ByteVector

import codec.byte.{ByteDecoder, ByteEncoder}
import failure.DecodingFailure

opaque type Utf8 = String
object Utf8:
  def from(s: String): Either[CharacterCodingException, Utf8] =
    ByteVector.encodeUtf8(s).map(_ => s)

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def unsafeFrom(s: String): Utf8 = from(s).fold(e => throw e, identity)

  extension (u: Utf8)
    def value: String = u
    def bytes: ByteVector = ByteVector.view(u.getBytes(StandardCharsets.UTF_8))

  given eq: Eq[Utf8] = Eq.fromUniversalEquals

  given utf8CirceDecoder: Decoder[Utf8] =
    Decoder.decodeString.emap(from(_).left.map(_.getMessage))
  given utf8CirceEncoder: Encoder[Utf8] = Encoder.encodeString

  given utf8CirceKeyDecoder: KeyDecoder[Utf8] =
    KeyDecoder.instance(from(_).toOption)
  given utf8CirceKeyEncoder: KeyEncoder[Utf8] = KeyEncoder.encodeKeyString

  given utf8ByteDecoder: ByteDecoder[Utf8] = ByteDecoder[BigNat].flatMap { (b: BigNat) =>
    ByteDecoder
      .fromFixedSizeBytes[ByteVector](b.toBigInt.toLong)(identity)
      .emap(_.decodeUtf8.left.map { e => DecodingFailure(e.getMessage) })
  }
  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  given utf8ByteEncoder: ByteEncoder[Utf8] = (utf8: String) =>
    val encoded = ByteVector.encodeUtf8(utf8) match
      case Right(v) => v
      case Left(e) => throw e
    ByteEncoder[BigNat].encode(BigNat.unsafeFromLong(encoded.size)) ++ encoded
