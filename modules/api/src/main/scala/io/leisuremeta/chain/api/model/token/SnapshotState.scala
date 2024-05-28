package io.leisuremeta.chain
package api.model
package token

import java.time.Instant

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

    given snapshotIdByteEncoder: ByteEncoder[SnapshotId] = BigNat.bignatByteEncoder
    given snapshotIdByteDecoder: ByteDecoder[SnapshotId] = BigNat.bignatByteDecoder

    val Zero: SnapshotId = BigNat.Zero

    extension (id: SnapshotId)
      def inc: SnapshotId = increase
      def increase: SnapshotId = BigNat.add(id, BigNat.One)
