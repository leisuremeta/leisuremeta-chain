package io.leisuremeta.chain
package api.model
package api_model

import java.time.Instant

import lib.crypto.Hash
import lib.datatype.{BigNat, Utf8}

final case class ActivityInfo(
    timestamp: Instant,
    point: BigInt,
    description: Utf8,
    txHash: Hash.Value[TransactionWithResult],
)
