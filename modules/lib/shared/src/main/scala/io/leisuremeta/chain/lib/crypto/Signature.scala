package io.leisuremeta.chain.lib
package crypto

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval
import eu.timepit.refined.refineV
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import io.circe.refined.*
import scodec.bits.ByteVector

import codec.byte.{ByteDecoder, ByteEncoder, DecodeResult}
import datatype.UInt256BigInt
import failure.DecodingFailure

final case class Signature(
    v: Signature.Header,
    r: UInt256BigInt,
    s: UInt256BigInt,
):
  override lazy val toString: String =
    s"Signature($v, ${r.toBytes.toHex}, ${s.toBytes.toHex})"

object Signature:

  type HeaderRange = Interval.Closed[27, 34]

  type Header = Int Refined HeaderRange

  given headerEncoder: ByteEncoder[Header] =
    ByteVector `fromByte` _.value.toByte

  given headerDecoder: ByteDecoder[Header] =
    ByteDecoder[Byte].decode(_).flatMap { case DecodeResult(b, remainder) =>
      refineV[Signature.HeaderRange](b.toInt)
        .map(DecodeResult(_, remainder))
        .left.map(DecodingFailure(_))
    }

  given sigEncoder: ByteEncoder[Signature] = ByteEncoder.derived

  given sigDecoder: ByteDecoder[Signature] = ByteDecoder.derived

  given sigCirceEncoder: Encoder[Signature] = deriveEncoder[Signature]

  given sigCirceDecoder: Decoder[Signature] = deriveDecoder[Signature]

