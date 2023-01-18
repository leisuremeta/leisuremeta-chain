package io.leisuremeta.chain
package api.model
package reward

import java.time.Instant

import lib.datatype.{BigNat, Utf8}

final case class ActivitySnapshot(
    account: Account,
    from: Instant,
    to: Instant,
    name: Utf8,
    weight: BigInt,
    count: BigNat,
    backlogs: Set[Signed.TxHash],
)
