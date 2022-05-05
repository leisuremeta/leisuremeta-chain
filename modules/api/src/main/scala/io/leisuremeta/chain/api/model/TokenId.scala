package io.leisuremeta.chain
package api.model

import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

import lib.datatype.Utf8

opaque type TokenId = Utf8
object TokenId:
  def apply(utf8: Utf8): TokenId = utf8

  given Decoder[TokenId] = Utf8.utf8CirceDecoder
  given Encoder[TokenId] = Utf8.utf8CirceEncoder
  given Schema[TokenId] = Schema.string
