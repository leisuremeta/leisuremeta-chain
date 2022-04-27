package org.leisuremeta.lmchain.core
package crypto

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval
import eu.timepit.refined.{W, refineV}
import scodec.bits.ByteVector

import codec.byte.{ByteDecoder, ByteEncoder, DecodeResult}
import datatype.UInt256BigInt
import failure.DecodingFailure

final case class Signature(
    v: Signature.Header,
    r: UInt256BigInt,
    s: UInt256BigInt,
) {
  override lazy val toString: String =
    s"Signature($v, ${r.toBytes.toHex}, ${s.toBytes.toHex})"
}

object Signature {

  type HeaderRange = Interval.Closed[W.`27`.T, W.`34`.T]

  type Header = Int Refined HeaderRange

  implicit val headerEncoder: ByteEncoder[Header] =
    ByteVector `fromByte` _.value.toByte

  implicit val headerDecoder: ByteDecoder[Header] = { bytes =>
    ByteDecoder[Byte].decode(bytes).flatMap { case DecodeResult(b, remainder) =>
      refineV[Signature.HeaderRange](b.toInt) match {
        case Left(msg)      => Left(DecodingFailure(msg))
        case Right(refined) => Right(DecodeResult(refined, remainder))
      }
    }
  }

  implicit val sigEncoder: ByteEncoder[Signature] = ByteEncoder.genericEncoder

  implicit val sigDecoder: ByteDecoder[Signature] = ByteDecoder.genericDecoder

}
