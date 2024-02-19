package io.leisuremeta.chain.lmscan.common.model

import java.util.Date
import java.time.Instant

final case class NftOwnerInfo(
    owner: Option[String] = None,
    nftUrl: Option[String] = None,
)
