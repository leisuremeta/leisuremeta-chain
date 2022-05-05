package io.leisuremeta.chain
package api.model

import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import sttp.tapir.Schema

import lib.datatype.Utf8

opaque type Rarity = Utf8
object Rarity:
  def apply(value: Utf8): Rarity = value

  given Encoder[Rarity] = Utf8.utf8CirceEncoder
  given Decoder[Rarity] = Utf8.utf8CirceDecoder

  given KeyEncoder[Rarity] = Utf8.utf8CirceKeyEncoder
  given KeyDecoder[Rarity] = Utf8.utf8CirceKeyDecoder
  given Schema[Rarity] = Schema.string
