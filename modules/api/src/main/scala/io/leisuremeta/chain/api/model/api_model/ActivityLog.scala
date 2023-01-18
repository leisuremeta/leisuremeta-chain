package io.leisuremeta.chain
package api.model.api_model

import java.time.Instant

import lib.datatype.{BigNat, Utf8}

final case class ActivityLog(
    timestamp: Instant,
    activityName: Utf8,
    weight: BigNat,
    count: BigNat,
)
