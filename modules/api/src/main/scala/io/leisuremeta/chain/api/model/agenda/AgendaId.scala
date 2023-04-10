package io.leisuremeta.chain
package api.model.agenda

import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import sttp.tapir.{Codec, DecodeResult, Schema}
import sttp.tapir.CodecFormat.TextPlain

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.Utf8

opaque type AgendaId = Utf8

object AgendaId:
  def apply(id: Utf8): AgendaId = id
  extension (id: AgendaId) def utf8: Utf8 = id

  given Decoder[AgendaId] = Utf8.utf8CirceDecoder
  given Encoder[AgendaId] = Utf8.utf8CirceEncoder
  given Schema[AgendaId] = Schema.string

  given KeyDecoder[AgendaId] = Utf8.utf8CirceKeyDecoder
  given KeyEncoder[AgendaId] = Utf8.utf8CirceKeyEncoder

  given ByteDecoder[AgendaId] = Utf8.utf8ByteDecoder.map(AgendaId(_))
  given ByteEncoder[AgendaId] = Utf8.utf8ByteEncoder.contramap(_.utf8)

  given Codec[String, AgendaId, TextPlain] = Codec.string.mapDecode{ (s: String) =>
    Utf8.from(s) match
      case Left(e) => DecodeResult.Error(s, e)
      case Right(a) => DecodeResult.Value(AgendaId(a))
  }(_.utf8.value)

  given cats.Eq[AgendaId] = cats.Eq.fromUniversalEquals
