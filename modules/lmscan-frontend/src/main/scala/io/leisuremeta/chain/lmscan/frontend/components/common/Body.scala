package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import Dom.{yyyy_mm_dd_time, timeAgo}
import V.*
import io.leisuremeta.chain.lmscan.common.model._

object Body:
  def blocks(payload: List[BlockInfo]) =
    payload.map(v =>
      div(`class` := "row table-body")(
        gen.cell(
          Cell.BLOCK_NUMBER(v.hash, v.number),
          Cell.AGE(v.createdAt),
          Cell.BLOCK_HASH(v.hash),
          Cell.PlainLong(v.txCount),
        ),
      ),
    )
  def txlist_txtable_off = (payload: List[TxInfo]) =>
    payload
      // List(new TxInfo)
      .map(v =>
        div(`class` := "row table-body")(
          gen.cell(
            Cell.TX_HASH(v.hash),
            Cell.PlainLong(v.blockNumber),
            Cell.AGE(v.createdAt),
            Cell.ACCOUNT_HASH(v.signer),
            Cell.Tx_VALUE((v.subType, V.validNull(v.value))),
          ),
        ),
      )
  def txlist_txtable_on = (payload: List[TxInfo]) =>
    payload
      // List(new TxInfo)
      .map(v =>
        div(`class` := "row table-body")(
          gen.cell(
            Cell.TX_HASH(v.hash),
            Cell.PlainLong(v.blockNumber),
            Cell.AGE(v.createdAt),
            Cell.ACCOUNT_HASH(v.signer),
            Cell.PlainStr(v.subType), // subtype 추가
            Cell.Tx_VALUE((v.subType, V.validNull(v.value))),
          ),
        ),
      )
  def dashboard_txtable = (payload: List[TxInfo]) =>
    payload
      .map(v =>
        div(`class` := "row table-body")(
          gen.cell(
            Cell.TX_HASH(v.hash),
            Cell.AGE(v.createdAt),
            Cell.ACCOUNT_HASH(v.signer),
          ),
        ),
      )

  def nft = (payload: List[NftActivity]) =>
    payload
      .map(v =>
        div(
          `class` := "row table-body",
        )(
          gen.cell(
            Cell.TX_HASH(v.txHash),
            Cell.AGE(v.createdAt),
            Cell.PlainStr(v.action),
            Cell.ACCOUNT_HASH(v.fromAddr),
            Cell.ACCOUNT_HASH(v.toAddr),
          ),
        ),
      )
