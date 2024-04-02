package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import V.*
import io.leisuremeta.chain.lmscan.common.model._

object Body:
  def blocks(payload: List[BlockInfo], g: GlobalModel) =
    payload.map(v =>
      div(cls := "row table-body")(
        gen.cell(
          Cell.BLOCK_NUMBER(v.hash, v.number),
          Cell.AGE(v.createdAt, g.current),
          Cell.BLOCK_HASH(v.hash),
          Cell.PlainLong(v.txCount),
        ),
      ),
    )
  def accs(payload: List[AccountInfo]) =
    payload.map(v =>
      div(cls := "row table-body")(
        gen.cell(
          Cell.ACCOUNT_HASH(v.address),
          Cell.Balance(v.balance),
          Cell.PriceS(v.value),
          Cell.DateS(v.updated),
        ),
      ),
    )
  def txRow = (payload: List[TxInfo], g: GlobalModel) =>
    payload.map(v =>
      div(cls := "row table-body")(
        gen.cell(
          Cell.TX_HASH(v.hash),
          Cell.PlainLong(v.blockNumber),
          Cell.AGE(v.createdAt, g.current),
          Cell.ACCOUNT_HASH(v.signer),
          Cell.PlainStr(v.subType),
        ),
      ),
    )
  def boardTxRow = (payload: List[TxInfo], g: GlobalModel) =>
    payload
      .map(v =>
        div(cls := "row table-body")(
          gen.cell(
            Cell.TX_HASH(v.hash),
            Cell.AGE(v.createdAt, g.current),
            Cell.ACCOUNT_HASH(v.signer),
          ),
        ),
      )

  def nfts = (payload: List[NftInfoModel]) =>
    payload
      .map(v =>
        div(
          cls := "row table-body",
        )(
          gen.cell(
            Cell.ImageS(v.thumbUrl),
            Cell.NftToken(v),
            Cell.PlainStr(v.totalSupply),
            Cell.DateS(v.startDate),
            Cell.DateS(v.endDate),
          ),
        ),
      )


  def nftToken = (payload: List[NftSeasonModel]) =>
    payload
      .map(v =>
        div(
          cls := "row table-body",
        )(
          gen.cell(
            Cell.NftDetail(v, v.nftName),
            Cell.Any(v.getCollection),
            Cell.NftDetail(v, v.tokenId),
            Cell.PlainStr(v.creator),
            Cell.PlainStr(v.rarity),
          ),
        ),
      )

  def nft = (payload: List[NftActivity], g: GlobalModel) =>
    payload
      .map(v =>
        div(
          cls := "row table-body",
        )(
          gen.cell(
            Cell.TX_HASH(v.txHash),
            Cell.AGE(v.createdAt, g.current),
            Cell.PlainStr(v.action),
            Cell.ACCOUNT_HASH(v.fromAddr),
            Cell.ACCOUNT_HASH(v.toAddr),
          ),
        ),
      )
