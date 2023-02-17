package io.leisuremeta.chain.lmscan.agent.model

final case class NftMetaInfo(
    Creator_description: String,
    Collection_description: String,
    Rarity: String,
    NFT_checksum: String,
    Collection_name: String,
    Creator: String,
    NFT_name: String,
    NFT_URI: String
)
