package io.leisuremeta.chain
package api.model
package account

import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import sttp.tapir.{Codec, DecodeResult, Schema}
import sttp.tapir.CodecFormat.TextPlain

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.Utf8

opaque type ExternalChainAddress = Utf8
object ExternalChainAddress:
  def apply(utf8: Utf8): ExternalChainAddress = utf8

  extension (a: ExternalChainAddress)
    def utf8: Utf8 = a

  given Decoder[ExternalChainAddress] = Utf8.utf8CirceDecoder
  given Encoder[ExternalChainAddress] = Utf8.utf8CirceEncoder
  given Schema[ExternalChainAddress] = Schema.string

  given KeyDecoder[ExternalChainAddress] = Utf8.utf8CirceKeyDecoder
  given KeyEncoder[ExternalChainAddress] = Utf8.utf8CirceKeyEncoder

  given ByteDecoder[ExternalChainAddress] = Utf8.utf8ByteDecoder.map(ExternalChainAddress(_))
  given ByteEncoder[ExternalChainAddress] = Utf8.utf8ByteEncoder.contramap(_.utf8)

  given Codec[String, ExternalChainAddress, TextPlain] = Codec.string.mapDecode{ (s: String) =>
    Utf8.from(s) match
      case Left(e) => DecodeResult.Error(s, e)
      case Right(a) => DecodeResult.Value(ExternalChainAddress(a))
  }(_.utf8.value)
