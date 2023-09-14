package io.leisuremeta.chain.lmscan.common.model

import java.util.Date
import java.time.Instant

final case class NftInfoModel(
    season: Option[String] = None,
    seasonName: Option[String] = None,
    totalSupply: Option[Int] = None,
    startDate: Option[Instant] = None,
    endDate: Option[Instant] = None,
    thumbUrl: Option[String] = None,
) extends ApiModel 
