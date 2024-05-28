package io.leisuremeta.chain
package api.model
package token

import java.time.Instant

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.{BigNat, Utf8}

final case class SnapshotState(
    snapshotId: SnapshotState.SnapshotId,
    createdAt: Instant,
    txHash: Signed.Tx,
    memo: Option[Utf8],
)

object SnapshotState:
  opaque type SnapshotId = BigNat
  object SnapshotId:
    def apply(value: BigNat): SnapshotId = value

    given snapshotIdByteEncoder: ByteEncoder[SnapshotId] = BigNat.bignatByteEncoder
    given snapshotIdByteDecoder: ByteDecoder[SnapshotId] = BigNat.bignatByteDecoder
