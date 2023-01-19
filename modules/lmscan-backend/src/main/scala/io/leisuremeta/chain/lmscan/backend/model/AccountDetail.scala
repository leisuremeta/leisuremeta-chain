package io.leisuremeta.chain.lmscan.backend.model

import io.leisuremeta.chain.lmscan.backend.entity.{Tx, Account}

final case class AccountDetail(
    address: String,
    balance: Double,
    value: Double,
    var txHistory: Seq[Tx],
):
  def this(acc: Account) =
    this(acc.address, acc.balance, acc.amount, null)
