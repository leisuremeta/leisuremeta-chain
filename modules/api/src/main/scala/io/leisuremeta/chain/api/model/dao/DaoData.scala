package io.leisuremeta.chain.api.model
package dao

final case class DaoData(
  daoAccount: Account,
  moderators: Set[Account],
  rewardRatio: RewardRatio,
  moderatorSelectionRule: ModeratorSelectionRule,
)
