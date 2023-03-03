package io.leisuremeta.chain.lmscan.common.model

final case class BlockInfo(
    number: Option[Long] = None,
    hash: Option[String] = None,
    txCount: Option[Long] = None,
    createdAt: Option[Long] = None,
)
