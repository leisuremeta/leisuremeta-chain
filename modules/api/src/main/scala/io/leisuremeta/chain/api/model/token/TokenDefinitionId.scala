package io.leisuremeta.chain
package api.model
package token

import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.Utf8

opaque type TokenDefinitionId = Utf8
object TokenDefinitionId:
  def apply(utf8: Utf8): TokenDefinitionId = utf8

  extension (a: TokenDefinitionId)
    def utf8: Utf8 = a

  given Decoder[TokenDefinitionId] = Utf8.utf8CirceDecoder
  given Encoder[TokenDefinitionId] = Utf8.utf8CirceEncoder
  given Schema[TokenDefinitionId] = Schema.string

  given ByteDecoder[TokenDefinitionId] = Utf8.utf8ByteDecoder.map(TokenDefinitionId(_))
  given ByteEncoder[TokenDefinitionId] = Utf8.utf8ByteEncoder.contramap(_.utf8)
