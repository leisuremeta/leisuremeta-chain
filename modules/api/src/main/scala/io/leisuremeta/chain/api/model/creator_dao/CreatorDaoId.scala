package io.leisuremeta.chain
package api.model
package creator_dao

import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import sttp.tapir.{Codec, DecodeResult, Schema}
import sttp.tapir.CodecFormat.TextPlain

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.Utf8

opaque type CreatorDaoId = Utf8
object CreatorDaoId:
  def apply(utf8: Utf8): CreatorDaoId = utf8

  extension (a: CreatorDaoId) def utf8: Utf8 = a

  given Decoder[CreatorDaoId] = Utf8.utf8CirceDecoder
  given Encoder[CreatorDaoId] = Utf8.utf8CirceEncoder
  given Schema[CreatorDaoId]  = Schema.string

  given KeyDecoder[CreatorDaoId] = Utf8.utf8CirceKeyDecoder
  given KeyEncoder[CreatorDaoId] = Utf8.utf8CirceKeyEncoder

  given ByteDecoder[CreatorDaoId] = Utf8.utf8ByteDecoder.map(CreatorDaoId(_))
  given ByteEncoder[CreatorDaoId] = Utf8.utf8ByteEncoder.contramap(_.utf8)

  given Codec[String, CreatorDaoId, TextPlain] = Codec.string.mapDecode {
    (s: String) =>
      Utf8.from(s) match
        case Left(e)  => DecodeResult.Error(s, e)
        case Right(a) => DecodeResult.Value(CreatorDaoId(a))
  }(_.utf8.value)

  given cats.Eq[CreatorDaoId] = cats.Eq.fromUniversalEquals
