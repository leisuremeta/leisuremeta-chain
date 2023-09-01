package io.leisuremeta.chain.lmscan.common.model

import java.util.Date
import java.time.Instant

final case class NftInfoModel(
    tokenDefId: Option[String] = None,
    season: Option[String] = None,
    collectionName: Option[String] = None,
    collectionSn: Option[Int] = None,
    totalSupply: Option[Int] = None,
    startDate: Option[Instant] = None,
    endDate: Option[Instant] = None,
    thumbUrl: Option[String] = None,
)
