package io.leisuremeta.chain.lmscan.backend.model

import io.leisuremeta.chain.lmscan.backend.entity.Account
import io.leisuremeta.chain.lmscan.backend.model.TxInfo

final case class AccountDetail(
    address: String,
    balance: BigInt,
    value: BigInt,
    txHistory: Seq[TxInfo],
):
  def this(acc: Account, txHist: Seq[TxInfo]) =
    this(acc.address, acc.balance.toBigInt, acc.amount.toBigInt, txHist)
