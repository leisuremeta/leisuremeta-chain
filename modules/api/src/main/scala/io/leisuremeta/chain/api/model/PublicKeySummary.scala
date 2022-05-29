package io.leisuremeta.chain
package api.model

import java.time.Instant

import cats.Eq
import cats.syntax.eq.given
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.Size
import eu.timepit.refined.generic.Equal
import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import scodec.bits.ByteVector
import sttp.tapir.Schema

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.crypto.{Hash, PublicKey}
import lib.datatype.Utf8

opaque type PublicKeySummary = ByteVector

object PublicKeySummary:

  final case class Info(
    description: Utf8,
    addedAt: Instant,
  )

  def apply(bytes: ByteVector): Either[String, PublicKeySummary] =
    Either.cond(
      bytes.size === 20,
      bytes,
      "PublicKeySummary must be 20 bytes",
    )

  def fromHex(hexString: String): Either[String, PublicKeySummary] =
    for
      bytes <- ByteVector.fromHexDescriptive(hexString)
      summary <- apply(bytes)
    yield summary

  def fromPublicKeyHash(hash: Hash.Value[PublicKey]): PublicKeySummary =
    hash.toUInt256Bytes.toBytes takeRight 20

  extension (pks: PublicKeySummary)
    def toBytes: ByteVector = pks

  given Eq[PublicKeySummary] = Eq.fromUniversalEquals

  given Decoder[PublicKeySummary] = Decoder.decodeString.emap { (s: String) =>
    val (f, b) = s `splitAt` 2
    for
      _ <- Either.cond(
        f === "0x",
        (),
        s"PublicKeySummary string not starting 0x: $f",
      )
      summary <- fromHex(b)
    yield summary
  }
  given Encoder[PublicKeySummary] =
    Encoder.encodeString.contramap(summary => s"0x${summary.toString}")
  
  given KeyDecoder[PublicKeySummary] = KeyDecoder.instance(fromHex(_).toOption)
  given KeyEncoder[PublicKeySummary] = KeyEncoder.encodeKeyString.contramap(_.toString)

  given Schema[PublicKeySummary] = Schema.string

  given ByteDecoder[PublicKeySummary] = ByteDecoder.fromFixedSizeBytes(20)(identity)
  given ByteEncoder[PublicKeySummary] = (bytes: ByteVector) => bytes
