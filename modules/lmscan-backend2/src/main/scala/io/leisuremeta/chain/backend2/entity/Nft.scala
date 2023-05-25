package io.leisuremeta.chain.lmscan.backend2.entity

// final case class Nft(
//     tokenId: String,
//     txHash: String,
//     action: String,
//     fromAddr: String,
//     toAddr: String,
//     eventTime: Long,
//     createdAt: Long,
// )

// key 값의 이름은 맞지않더라도, type은 db의 type 순서대로 맞아야하
final case class Nft(
    txHash: String,
    action: String,
    fromAddr: String,
    toAddr: String,
    eventTime: Long,
    createdAt: Long,
    tokenId: String,
)

// final case class NftActivity(
//     txHash: String,
//     action: String,
//     fromAddr: String,
//     toAddr: String,
//     createdAt: Long,
// )
