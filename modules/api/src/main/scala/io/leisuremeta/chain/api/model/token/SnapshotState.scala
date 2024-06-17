package io.leisuremeta.chain
package api.model
package token

import java.time.Instant
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import sttp.tapir.{Codec, DecodeResult, Schema}
import sttp.tapir.CodecFormat.TextPlain

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.{BigNat, Utf8}

final case class SnapshotState(
    snapshotId: SnapshotState.SnapshotId,
    createdAt: Instant,
    txHash: Signed.TxHash,
    memo: Option[Utf8],
)

object SnapshotState:
  opaque type SnapshotId = BigNat
  object SnapshotId:
    def apply(value: BigNat): SnapshotId = value

    given snapshotIdByteEncoder: ByteEncoder[SnapshotId] =
      BigNat.bignatByteEncoder
    given snapshotIdByteDecoder: ByteDecoder[SnapshotId] =
      BigNat.bignatByteDecoder

    given snapshotIdCirceDecoder: Decoder[SnapshotId] =
      BigNat.bignatCirceDecoder
    given snapshotIdCirceEncoder: Encoder[SnapshotId] =
      BigNat.bignatCirceEncoder

    given schema: Schema[SnapshotId] = Schema.schemaForBigInt
      .map[BigNat]: (bigint: BigInt) =>
        BigNat.fromBigInt(bigint).toOption
      .apply: (bignat: BigNat) =>
        bignat.toBigInt

    given tapirCodec: Codec[String, SnapshotId, TextPlain] =
      Codec.string
        .mapDecode: (s: String) =>
          BigNat.fromBigInt(BigInt(s)) match
            case Right(bignat) => DecodeResult.Value(bignat)
            case Left(msg)     => DecodeResult.Error(s, new Exception(msg))
        .apply: (bignat: BigNat) =>
          bignat.toBigInt.toString(10)

    val Zero: SnapshotId = BigNat.Zero

    extension (id: SnapshotId)
      def inc: SnapshotId      = increase
      def increase: SnapshotId = BigNat.add(id, BigNat.One)
