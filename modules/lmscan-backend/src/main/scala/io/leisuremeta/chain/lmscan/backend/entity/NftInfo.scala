package io.leisuremeta.chain.lmscan.backend.entity

import java.util.Date

final case class NftInfo(
    season: String,
    seasonName: String,
    totalSupply: Option[Int],
    startDate: Option[Date],
    endDate: Option[Date],
    thumbUrl: Option[String],
    sort: Int,
)
