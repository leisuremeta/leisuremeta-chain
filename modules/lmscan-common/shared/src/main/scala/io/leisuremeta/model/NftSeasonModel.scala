package io.leisuremeta.chain.lmscan.common.model

import java.util.Date
import java.time.Instant

final case class NftSeasonModel(
    nftName: Option[String] = None,
    tokenId: Option[String] = None,
    tokenDefId: Option[String] = None,
    creator: Option[String] = None,
    rarity: Option[String] = None,
    thumbUrl: Option[String] = None,
)
