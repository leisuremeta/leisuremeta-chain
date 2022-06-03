package io.leisuremeta.chain
package api.model

import io.circe.{Decoder, Encoder}
import sttp.tapir.{Codec, DecodeResult, Schema}
import sttp.tapir.CodecFormat.TextPlain

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.Utf8

opaque type GroupId = Utf8
object GroupId:
  def apply(utf8: Utf8): GroupId = utf8

  extension (a: GroupId)
    def utf8: Utf8 = a

  given Encoder[GroupId] = Encoder.encodeString.contramap(_.utf8.value)
  given Decoder[GroupId] = Decoder.decodeString.emap(Utf8.from(_).left.map(_.getMessage)).map(apply)
  given Schema[GroupId] = Schema.string

  given ByteDecoder[GroupId] = Utf8.utf8ByteDecoder.map(GroupId(_))
  given ByteEncoder[GroupId] = Utf8.utf8ByteEncoder.contramap(_.utf8)

  given Codec[String, GroupId, TextPlain] = Codec.string.mapDecode{ (s: String) =>
    Utf8.from(s) match
      case Left(e) => DecodeResult.Error(s, e)
      case Right(a) => DecodeResult.Value(GroupId(a))
  }(_.utf8.value)
