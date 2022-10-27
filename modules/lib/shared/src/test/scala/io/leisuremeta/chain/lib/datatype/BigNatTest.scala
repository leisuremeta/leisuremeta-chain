package io.leisuremeta.chain.lib
package datatype

import eu.timepit.refined.auto.autoUnwrap
import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import io.circe.generic.auto.given
import io.circe.refined.given
import io.circe.syntax.given
import scodec.bits.ByteVector

import hedgehog.munit.HedgehogSuite
import hedgehog.*

import codec.byte.{ByteDecoder, ByteEncoder, DecodeResult}

class BigNatTest extends HedgehogSuite:

  property("roundtrip of bignat byte codec") {
    for
      bignat <- Gen
        .bytes(Range.linear(0, 64))
        .map(BigInt(1, _))
        .map(BigNat.unsafeFromBigInt)
        .forAll
    yield
      val encoded = ByteEncoder[BigNat].encode(bignat)

      ByteDecoder[BigNat].decode(encoded) match
        case Right(DecodeResult(decoded, remainder)) =>
          Result.all(
            List(
              decoded ==== bignat,
              Result.assert(remainder.isEmpty),
            ),
          )
        case _ => Result.failure
  }

  property("roundtrip of bignat circe codec") {
    for
      bignat <- Gen
        .bytes(Range.linear(0, 64))
        .map(BigInt(1, _))
        .map(BigNat.unsafeFromBigInt)
        .forAll
    yield
      val encoded = bignat.asJson

      Decoder[BigNat].decodeJson(encoded) match
        case Right(decoded) => decoded ==== bignat
        case _ => Result.failure
  }
