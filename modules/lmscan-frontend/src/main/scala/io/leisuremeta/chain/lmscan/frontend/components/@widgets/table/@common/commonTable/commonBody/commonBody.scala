package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import ValidOutputData.*
import Dom.*

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
