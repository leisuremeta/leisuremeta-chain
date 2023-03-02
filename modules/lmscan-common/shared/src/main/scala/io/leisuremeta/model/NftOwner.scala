package io.leisuremeta.chain.lmscan.backend.entity

final case class NftOwnerModel(
    tokenId: Option[String],
    owner: Option[String],
    createdAt: Option[Long],
    eventTime: Option[Long],
)
