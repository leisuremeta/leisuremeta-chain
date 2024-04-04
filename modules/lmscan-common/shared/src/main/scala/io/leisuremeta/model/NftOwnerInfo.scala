package io.leisuremeta.chain.lmscan.common.model

final case class NftOwnerInfo(
    owner: Option[String] = None,
    nftUrl: Option[String] = None,
)
