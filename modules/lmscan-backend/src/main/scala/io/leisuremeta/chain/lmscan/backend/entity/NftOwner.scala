package io.leisuremeta.chain.lmscan.backend.entity

final case class NftOwner(
    tokenId: String = "",
    owner: String = "",
    createdAt: Long = 0,
    eventTime: Long = 0,
)
