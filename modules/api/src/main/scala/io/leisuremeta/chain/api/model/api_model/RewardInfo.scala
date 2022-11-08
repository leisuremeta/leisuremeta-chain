package io.leisuremeta.chain
package api.model
package api_model

import java.time.Instant

import lib.datatype.BigNat
import token.Rarity
import reward.DaoActivity

final case class RewardInfo(
    account: Account,
    reward: RewardInfo.Reward,
    point: RewardInfo.Point,
    timestamp: Instant,
    totalNumberOfDao: BigNat,
)

object RewardInfo:

  final case class Reward(
      total: BigNat,
      activity: BigNat,
      token: BigNat,
      rarity: BigNat,
      bonus: BigNat,
  )

  final case class Point(
      activity: DaoActivity,
      token: DaoActivity,
      rarity: Map[Rarity, BigNat],
  )
