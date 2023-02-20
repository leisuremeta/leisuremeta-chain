package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.circe.*, io.circe.parser.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import Dom.{_hidden, isEqGet, yyyy_mm_dd_time, timeAgo}
import V.*

object Body:
  def block = (payload: List[Block]) =>
    payload
      .map(v =>
        div(`class` := "row table-body")(
          gen.cell(
            Cell.BLOCK_NUMBER(v.hash, v.number),
            Cell.AGE(v.createdAt),
            Cell.BLOCK_HASH(v.hash),
            Cell.PlainInt(v.txCount),
          ),
        ),
      )

  def nft = (payload: List[NftActivities]) =>
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
  def blockDetail_txtable = (payload: List[Tx]) =>
    payload
      .map(v =>
        div(`class` := "row table-body")(
          gen.cell(
            Cell.TX_HASH(v.hash),
            Cell.PlainInt(v.blockNumber),
            Cell.AGE(v.createdAt),
            Cell.ACCOUNT_HASH(v.signer),
            // Cell.PlainStr(v.txType),
            // Cell.PlainStr(v.tokenType),
            Cell.Tx_VALUE((v.tokenType, v.value)),
          ),
        ),
      )
  def txlist_txtable = (payload: List[Tx]) =>
    payload
      .map(v =>
        div(`class` := "row table-body")(
          gen.cell(
            Cell.TX_HASH10(v.hash),
            Cell.PlainInt(v.blockNumber),
            Cell.AGE(v.createdAt),
            Cell.ACCOUNT_HASH(v.signer),
            // Cell.PlainStr(v.txType),
            // Cell.PlainStr(v.tokenType),
            Cell.Tx_VALUE((v.tokenType, v.value)),
          ),
        ),
      )

  def accountDetail_txtable = (payload: List[Tx]) =>
    payload
      .map(v =>
        div(`class` := "row table-body")(
          gen.cell(
            Cell.TX_HASH(v.hash),
            Cell.PlainInt(v.blockNumber),
            Cell.AGE(v.createdAt),
            Cell.ACCOUNT_HASH(v.signer),
            // Cell.PlainStr(v.txType),
            // Cell.PlainStr(v.tokenType),
            Cell.Tx_VALUE2((v.tokenType, v.value, v.inOut)),
          ),
        ),
      )
  def dashboard_txtable = (payload: List[Tx]) =>
    payload
      .map(v =>
        div(`class` := "row table-body")(
          gen.cell(
            Cell.TX_HASH10(v.hash),
            Cell.AGE(v.createdAt),
            Cell.ACCOUNT_HASH(v.signer),
          ),
        ),
      )
