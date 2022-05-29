package io.leisuremeta.chain
package api.model

import cats.Eq

import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import sttp.tapir.{Codec, DecodeResult, Schema}
import sttp.tapir.CodecFormat.TextPlain

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

  given Codec[String, Account, TextPlain] = Codec.string.mapDecode{ (s: String) =>
    Utf8.from(s) match
      case Left(e) => DecodeResult.Error(s, e)
      case Right(a) => DecodeResult.Value(Account(a))
  }(_.utf8.value)
  given Schema[Account] = Schema.string

  given ByteDecoder[Account] = Utf8.utf8ByteDecoder
  given ByteEncoder[Account] = Utf8.utf8ByteEncoder

  given Eq[Account] = Eq.fromUniversalEquals
