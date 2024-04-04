package io.leisuremeta.chain.lib
package codec

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
