package io.leisuremeta.chain.lib
package crypto

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval
import eu.timepit.refined.refineV
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

  @SuppressWarnings(Array("org.wartremover.warts.Nothing"))
  given headerDecoder: ByteDecoder[Header] = bytes =>
    ByteDecoder[Byte].decode(bytes).flatMap { case DecodeResult(b, remainder) =>
      refineV[Signature.HeaderRange](b.toInt) match
        case Left(msg)      => Left(DecodingFailure(msg))
        case Right(refined) => Right(DecodeResult(refined, remainder))
    }

  given sigEncoder: ByteEncoder[Signature] = ByteEncoder.genericEncoder

  given sigDecoder: ByteDecoder[Signature] = ByteDecoder.genericDecoder
