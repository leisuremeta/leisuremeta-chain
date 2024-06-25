package io.leisuremeta.chain
package api.model.vote

import cats.Eq
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

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
