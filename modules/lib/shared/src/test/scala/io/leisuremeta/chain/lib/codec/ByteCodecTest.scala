package io.leisuremeta.chain.lib
package codec

import eu.timepit.refined.auto.autoUnwrap
import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import io.circe.generic.auto.given
import io.circe.refined.given
import io.circe.syntax.given
import scodec.bits.ByteVector

import hedgehog.munit.HedgehogSuite
import hedgehog.*

import codec.byte.{ByteDecoder, ByteEncoder, DecodeResult}

class ByteCodecTest extends HedgehogSuite:

  property("roundtrip of bigint byte codec") {
    for
      bignat <- Gen
        .bytes(Range.linear(1, 64))
        .map(BigInt(_))
        .forAll
    yield
      val encoded = ByteEncoder[BigInt].encode(bignat)

      ByteDecoder[BigInt].decode(encoded) match
        case Right(DecodeResult(decoded, remainder)) =>
          Result.all(
            List(
              decoded ==== bignat,
              Result.assert(remainder.isEmpty),
            ),
          )
        case _ => Result.failure
  }
