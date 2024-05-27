package io.leisuremeta.chain
package api.model
package token

import java.time.Instant

import lib.datatype.BigNat

final case class SnapshotState(
    snapshotId: BigNat,
    createdAt: Instant,
)
