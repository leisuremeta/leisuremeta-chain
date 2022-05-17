package io.leisuremeta.chain
package api.model

import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.BigNat

opaque type NetworkId = BigNat

object NetworkId:
  def apply(value: BigNat): NetworkId = value

  given ByteDecoder[NetworkId] = BigNat.bignatByteDecoder
  given ByteEncoder[NetworkId] = BigNat.bignatByteEncoder

  given Decoder[NetworkId] = BigNat.bignatCirceDecoder
  given Encoder[NetworkId] = BigNat.bignatCirceEncoder

  given Schema[NetworkId] = Schema.schemaForBigInt.map[NetworkId] {
    (bigint: BigInt) =>
      BigNat.fromBigInt(bigint).toOption.map(apply)
  }{(bignat: BigNat) => bignat.toBigInt}
