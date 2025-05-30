package io.leisuremeta.chain.lmscan.backend.entity

final case class Summary(
    lmPrice: Double,
    blockNumber: Long,
    totalAccounts: Long,
    createdAt: Long,
    totalTxSize: BigDecimal,
    totalBalance: BigDecimal,
    marketCap: Option[BigDecimal],
    cirSupply: Option[BigDecimal],
    totalNft: Option[Long],
)
