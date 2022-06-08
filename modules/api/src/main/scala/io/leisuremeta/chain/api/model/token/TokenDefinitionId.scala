package io.leisuremeta.chain
package api.model
package token

import io.circe.{Decoder, Encoder}
import sttp.tapir.{Codec, DecodeResult, Schema}
import sttp.tapir.CodecFormat.TextPlain

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

  given Codec[String, TokenDefinitionId, TextPlain] = Codec.string.mapDecode{ (s: String) =>
    Utf8.from(s) match
      case Left(e) => DecodeResult.Error(s, e)
      case Right(a) => DecodeResult.Value(TokenDefinitionId(a))
  }(_.utf8.value)
