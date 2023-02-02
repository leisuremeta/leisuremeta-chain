package io.leisuremeta.chain.lmscan.agent.entity


final case class Account(
    address: String,
    balance: Double,
    amount: Double,
    createdAt: Long,
) 
