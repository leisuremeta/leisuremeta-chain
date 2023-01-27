package io.leisuremeta.chain.lmscan.backend.model

import io.leisuremeta.chain.lmscan.backend.entity.{Tx, Account}

final case class AccountDetail(
    address: String,
    balance: Double,
    value: Double,
    txHistory: Seq[Tx],
):
  def this(acc: Account, txHist: Seq[Tx]) =
    this(acc.address, acc.balance, acc.amount, txHist)
