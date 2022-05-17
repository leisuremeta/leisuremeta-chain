package io.leisuremeta.chain
package api.model

import cats.syntax.eq.given
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.Size
import eu.timepit.refined.generic.Equal
import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import scodec.bits.ByteVector
import sttp.tapir.Schema

opaque type PublicKeySummary = ByteVector

object PublicKeySummary:
  def apply(bytes: ByteVector): Either[String, PublicKeySummary] =
    Either.cond(
      bytes.size === 32,
      bytes,
      "PublicKeySummary must be 32 bytes",
    )

  def fromHex(hexString: String): Either[String, PublicKeySummary] =
    for
      bytes <- ByteVector.fromHexDescriptive(hexString)
      summary <- apply(bytes)
    yield summary

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
