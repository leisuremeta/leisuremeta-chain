package io.leisuremeta.chain
package api.model.voting

import cats.Eq
import io.circe.{Decoder, Encoder}
import sttp.tapir.{Codec, DecodeResult, Schema}
import sttp.tapir.CodecFormat.TextPlain

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.Utf8

opaque type ProposalId = Utf8

object ProposalId:

  def apply(utf8: Utf8): ProposalId = utf8
  extension (proposalId: ProposalId)
    def value: Utf8 = proposalId
  end extension

  given byteEncoder: ByteEncoder[ProposalId] = Utf8.utf8ByteEncoder
  given byteDecoder: ByteDecoder[ProposalId] = Utf8.utf8ByteDecoder

  given circeEncoder: Encoder[ProposalId] = Utf8.utf8CirceEncoder
  given circeDecoder: Decoder[ProposalId] = Utf8.utf8CirceDecoder

  given eq: Eq[ProposalId] = Eq.fromUniversalEquals

  given schema: Schema[ProposalId] = Schema.string

  given bignatCodec: Codec[String, ProposalId, TextPlain] =
    Codec.string
      .mapDecode: (s: String) =>
        Utf8.from(s) match
          case Left(e)  => DecodeResult.Error(s, e)
          case Right(v) => DecodeResult.Value(ProposalId(v))
      .apply: (b: ProposalId) =>
        b.value.value
