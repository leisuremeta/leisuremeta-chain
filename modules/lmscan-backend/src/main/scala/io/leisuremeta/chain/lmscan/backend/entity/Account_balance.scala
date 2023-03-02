package io.leisuremeta.chain.lmscan.backend.entity

// final case class AccountBalance(
//     address: String,
//     balance: BigDecimal,
//     createdAt: Long,
// )
final case class Balance(
    address: String,
    free: BigDecimal,
    locked: BigDecimal,
    updated_at: Long,
    created_at: Long,
)
