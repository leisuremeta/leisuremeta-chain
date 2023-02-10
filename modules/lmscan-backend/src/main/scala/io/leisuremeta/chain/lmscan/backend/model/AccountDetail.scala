package io.leisuremeta.chain.lmscan.backend.model

import io.leisuremeta.chain.lmscan.backend.entity.Account
import io.leisuremeta.chain.lmscan.backend.model.TxInfo

final case class AccountDetail(
    address: String,
    balance: Long,
    value: Long,
    txHistory: Seq[TxInfo],
):
  def this(acc: Account, txHist: Seq[TxInfo]) =
    this(acc.address, acc.balance, acc.amount, txHist)
