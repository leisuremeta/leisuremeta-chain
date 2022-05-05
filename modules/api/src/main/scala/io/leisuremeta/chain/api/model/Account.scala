package io.leisuremeta.chain
package api.model

import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import sttp.tapir.Schema

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.Utf8

opaque type Account = Utf8
object Account:
  def apply(value: Utf8): Account = value

  extension (a: Account)
    def utf8: Utf8 = a

  given Encoder[Account] = Encoder.encodeString.contramap(_.utf8.value)
  given Decoder[Account] = Decoder.decodeString.emap(Utf8.from(_).left.map(_.getMessage)).map(apply)

  given KeyDecoder[Account] = Utf8.utf8CirceKeyDecoder
  given KeyEncoder[Account] = Utf8.utf8CirceKeyEncoder

  given Schema[Account] = Schema.string
