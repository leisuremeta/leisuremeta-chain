package io.leisuremeta.chain
package api.model

import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

import lib.datatype.Utf8

opaque type TokenDefinitionId = Utf8
object TokenDefinitionId:
  def apply(utf8: Utf8): TokenDefinitionId = utf8

  given Decoder[TokenDefinitionId] = Utf8.utf8CirceDecoder
  given Encoder[TokenDefinitionId] = Utf8.utf8CirceEncoder
  given Schema[TokenDefinitionId] = Schema.string
