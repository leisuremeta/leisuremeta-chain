package io.leisuremeta.chain
package api.model
package account

import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import sttp.tapir.{Codec, DecodeResult, Schema}
import sttp.tapir.CodecFormat.TextPlain

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.Utf8

opaque type EthAddress = Utf8
object EthAddress:
  def apply(utf8: Utf8): EthAddress = utf8

  extension (a: EthAddress)
    def utf8: Utf8 = a

  given Decoder[EthAddress] = Utf8.utf8CirceDecoder
  given Encoder[EthAddress] = Utf8.utf8CirceEncoder
  given Schema[EthAddress] = Schema.string

  given KeyDecoder[EthAddress] = Utf8.utf8CirceKeyDecoder
  given KeyEncoder[EthAddress] = Utf8.utf8CirceKeyEncoder

  given ByteDecoder[EthAddress] = Utf8.utf8ByteDecoder.map(EthAddress(_))
  given ByteEncoder[EthAddress] = Utf8.utf8ByteEncoder.contramap(_.utf8)

  given Codec[String, EthAddress, TextPlain] = Codec.string.mapDecode{ (s: String) =>
    Utf8.from(s) match
      case Left(e) => DecodeResult.Error(s, e)
      case Right(a) => DecodeResult.Value(EthAddress(a))
  }(_.utf8.value)

