package io.leisuremeta.chain
package api.model
package token

import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import sttp.tapir.Schema

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.Utf8

opaque type Rarity = Utf8
object Rarity:
  def apply(value: Utf8): Rarity = value

  extension (a: Rarity) def utf8: Utf8 = a

  given Encoder[Rarity] = Utf8.utf8CirceEncoder
  given Decoder[Rarity] = Utf8.utf8CirceDecoder

  given KeyEncoder[Rarity] = Utf8.utf8CirceKeyEncoder
  given KeyDecoder[Rarity] = Utf8.utf8CirceKeyDecoder
  given Schema[Rarity]     = Schema.string

  given ByteDecoder[Rarity] = Utf8.utf8ByteDecoder.map(Rarity(_))
  given ByteEncoder[Rarity] = Utf8.utf8ByteEncoder.contramap(_.utf8)
