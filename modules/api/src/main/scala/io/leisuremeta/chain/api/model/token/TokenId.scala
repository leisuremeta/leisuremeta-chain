package io.leisuremeta.chain
package api.model
package token

import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import sttp.tapir.{Codec, DecodeResult, Schema}
import sttp.tapir.CodecFormat.TextPlain

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.Utf8

opaque type TokenId = Utf8
object TokenId:
  def apply(utf8: Utf8): TokenId = utf8

  extension (a: TokenId)
    def utf8: Utf8 = a

  given Decoder[TokenId] = Utf8.utf8CirceDecoder
  given Encoder[TokenId] = Utf8.utf8CirceEncoder
  given Schema[TokenId] = Schema.string

  given KeyDecoder[TokenId] = Utf8.utf8CirceKeyDecoder
  given KeyEncoder[TokenId] = Utf8.utf8CirceKeyEncoder

  given ByteDecoder[TokenId] = Utf8.utf8ByteDecoder.map(TokenId(_))
  given ByteEncoder[TokenId] = Utf8.utf8ByteEncoder.contramap(_.utf8)

  given Codec[String, TokenId, TextPlain] = Codec.string.mapDecode{ (s: String) =>
    Utf8.from(s) match
      case Left(e) => DecodeResult.Error(s, e)
      case Right(a) => DecodeResult.Value(TokenId(a))
  }(_.utf8.value)
