package io.leisuremeta.chain.lmscan.agent.entity

import io.leisuremeta.chain.api.model.Transaction.AccountTx.*
import java.time.Instant


final case class AccountEntity(
    address: String,
    balance: Double,
    amount: Double,
    eventTime: Long,
    createdAt: Long,
) 

object AccountEntity:
  def from(tx: CreateAccount) =
    AccountEntity(
      tx.account.utf8.value,
      0,
      0,
      tx.createdAt.getEpochSecond(),
      Instant.now().getEpochSecond(),
    )