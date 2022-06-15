package io.leisuremeta.chain
package api.model.dao

import lib.datatype.BigNat

import RewardRatio.*

final case class RewardRatio(
  overall: OverallRatio,
  activity: ActivityRatio,
  collector: CollectorRatio,
)

object RewardRatio:

  final case class OverallRatio(
    activity: BigNat,
    collector: BigNat,
    staking: BigNat,
  )

  final case class ActivityRatio(
    like: BigNat,
    comment: BigNat,
    referral: BigNat,
    report: BigNat,
  )
  object ActivityRatio:
    val default = ActivityRatio(BigNat.One, BigNat.One, BigNat.One, BigNat.One)

  final case class CollectorRatio(
    like: BigNat,
    comment: BigNat,
    referral: BigNat,
    report: BigNat,
  )
  object CollectorRatio:
    val default = CollectorRatio(BigNat.One, BigNat.One, BigNat.One, BigNat.One)