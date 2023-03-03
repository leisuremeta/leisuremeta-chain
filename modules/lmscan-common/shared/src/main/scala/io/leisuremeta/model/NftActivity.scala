package io.leisuremeta.chain.lmscan.common.model

final case class NftActivity(
    txHash: Option[String] = None,
    action: Option[String] = None,
    fromAddr: Option[String] = None,
    toAddr: Option[String] = None,
    createdAt: Option[Long] = None,
)
