package io.leisuremeta.chain.lmscan.common.model

final case class SummaryModel(
  id: Option[Long] = None,
  lmPrice: Option[Double] = None,
  blockNumber: Option[Long] = None,
  txCountInLatest24h: Option[Long] = None,
  totalAccounts: Option[Long] = None,
  createdAt: Option[Long] = None,
)
