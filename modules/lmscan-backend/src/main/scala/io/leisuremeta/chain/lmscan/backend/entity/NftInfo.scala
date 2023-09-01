package io.leisuremeta.chain.lmscan.backend.entity

import java.util.Date

final case class NftInfo(
    tokenDefId: String,
    season: String,
    collectionName: String,
    collectionSn: Int,
    totalSupply: Option[Int],
    startDate: Option[Date],
    endDate: Option[Date],
    infoProg: Option[Int],
    thumbUrl: Option[String],
)
