package io.leisuremeta.chain.lmscan
package frontend

import tyrian.Html.*
import tyrian.*
import common.model.*

object Table:
  def mainView(model: BaseModel): Html[Msg] =
    div(`class` := "table-area main-table")(
      div(
        `class` := "app-table blc table-container position-relative y-center",
      )(
        div(
          `class` := s"table-title",
        )(
          div(
            `class` := s"type-1",
          )(span("Latest Blocks")),
          div(
            `class` := s"type-2",
          )(
            span(
              onClick(ToPage(BlcModel(page = 1))),
            )("More"),
          ),
        ),
        model.blcs match
          case None => LoaderView.view
          case Some(v) => div(`class` := "w-[100%]")(Head.block :: Body.blocks(v.payload.toList)),
      ),
      div(
        `class` := "app-table tx-m table-container position-relative y-center",
      )(
        div(
          `class` := s"table-title",
        )(
          div(
            `class` := s"type-1",
          )(span("Latest transactions")),
          div(
            `class` := s"type-2",
          )(
            span(
              onClick(ToPage(TxModel(page = 1))),
            )("More"),
          ),
        ),
        model.txs match
          case None => LoaderView.view
          case Some(v) => div(Head.tx_dashBoard :: Body.boardTxRow(v.payload.toList)),
      ),
    )

  def view(model: BlcModel) =
    div(`class` := "table-container app-table blc")(
      block(model.data),
      Pagination.view(model),
      model.data match
        case None    => LoaderView.view
        case Some(_) => div(),
    )
  def view(model: AccModel) =
    div(`class` := "table-container app-table accs")(
      acc(model.data),
      Pagination.view(model),
      model.data match
        case None    => LoaderView.view
        case Some(_) => div(),
    )
  def view(model: NftModel) =
    div(`class` := "table-container app-table nfts")(
      nft(model.data),
      Pagination.view(model),
      model.data match
        case None    => LoaderView.view
        case Some(_) => div(),
    )
  def view(model: NftTokenModel) =
    div(`class` := "table-container app-table nft-token")(
      nftToken(model.data),
      Pagination.view(model),
      model.data match
        case None    => LoaderView.view
        case Some(_) => div(),
    )

  def view(model: TxModel): Html[Msg] =
    div(`class` := "table-container app-table tx")(
      model.data match
        case None => LoaderView.view
        case Some(v) =>
          div()(
            Head.tx :: Body.txRow(v.payload.toList),
          )
      ,
      Pagination.view(model),
    )
  def view(model: BlockDetail): Html[Msg] =
    div(`class` := "table-container app-table mt-15 tx")(
      model.txs match
        case None    => List(LoaderView.view)
        case Some(v) => Table.view(v),
    )
  def view(model: AccountDetail): Html[Msg] =
    div(`class` := "table-container app-table tx")(
      model.txHistory match
        case None    => List(LoaderView.view)
        case Some(v) => Table.view(v),
    )
  def view(model: NftDetail): Html[Msg] =
    div(`class` := "table-container app-table nft")(
      model.activities match
        case None    => List(Head.nft, LoaderView.view)
        case Some(v) => Head.nft :: Body.nft(v.toList),
    )

  def block(data: Option[PageResponse[BlockInfo]]) =
    div(
      data match
        case Some(v) => Head.block :: Body.blocks(v.payload.toList)
        case None    => List(Head.block),
    )
  def acc(data: Option[PageResponse[AccountInfo]]) =
    div(
      data match
        case Some(v) => Head.accs :: Body.accs(v.payload.toList)
        case None    => List(Head.accs),
    )
  def nft(list: Option[PageResponse[NftInfoModel]]) =
    div(
      list match
        case Some(v) => Head.nfts :: Body.nfts(v.payload.toList)
        case None    => List(Head.nfts),
    )
  def nftToken(list: Option[PageResponse[NftSeasonModel]]) =
    div(
      list match
        case Some(v) => Head.nftToken :: Body.nftToken(v.payload.toList)
        case None    => List(Head.nftToken),
    )
  def view(list: Seq[TxInfo]) =
    Head.tx :: Body.txRow(list.toList)
