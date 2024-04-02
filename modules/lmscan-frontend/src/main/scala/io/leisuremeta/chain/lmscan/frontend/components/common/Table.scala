package io.leisuremeta.chain.lmscan
package frontend

import tyrian.Html.*
import tyrian.*
import common.model.*

object Table:
  def mainView(model: BaseModel): Html[Msg] =
    div(cls := "table-area main-table")(
      div(
        cls := "blc table-container",
      )(
        div(cls := "table-title")(
          label(text("Latest Blocks"), input(name := "toggle-main", typ := "radio", checked)),
          a(onClick(ToPage(BlcModel(page = 1))))("More"),
        ) ::
        (model.blcs match
          case None => List(LoaderView.view)
          case Some(v) => Head.block :: Body.blocks(v.payload.toList, model.global))
      ),
      div(
        cls := "tx-m table-container",
      )(
        div(cls := "table-title")(
          label(text("Latest transactions"), input(name := "toggle-main", typ := "radio")),
          a(onClick(ToPage(TxModel(page = 1))))("More"),
        ) ::
        (model.txs match
          case None => List(LoaderView.view)
          case Some(v) => Head.tx_dashBoard :: Body.boardTxRow(v.payload.toList, model.global))
      ),
    )

  def view(model: BlcModel) =
    div(cls := "table-container blc")(
      Head.block ::
      (model.data match
        case Some(v) =>  Body.blocks(v.payload.toList, model.global).appended(Pagination.view(model))
        case None    => List(LoaderView.view)),
    )
  def view(model: AccModel) =
    div(cls := "table-container accs")(
      model.data match
        case None    => List(LoaderView.view)
        case Some(data) => 
          Head.accs :: Body.accs(data.payload.toList).appended(Pagination.view(model))
    )
  def view(model: NftModel) =
    div(cls := "table-container nfts")(
      nft(model.data),
      Pagination.view(model),
      model.data match
        case None    => LoaderView.view
        case Some(_) => div(),
    )
  def view(model: NftTokenModel) =
    div(cls := "table-container nft-token")(
      nftToken(model.data),
      Pagination.view(model),
      model.data match
        case None    => LoaderView.view
        case Some(_) => div(),
    )

  def view(model: TxModel): Html[Msg] =
    div(cls := "table-container tx")(
      Head.tx :: 
      (model.data match
        case None => List(LoaderView.view)
        case Some(v) =>
          Body.txRow(v.payload.toList, model.global).appended(Pagination.view(model))
      ),
    )
  def view(model: BlcDetailModel): Html[Msg] =
    div(cls := "table-container tx")(
      Head.tx :: Body.txRow(model.blcDetail.payload.toList, model.global).appended(Pagination.view(model))
    )
  def view(model: AccDetailModel): Html[Msg] =
    div(cls := "table-container tx")(
      Head.tx :: Body.txRow(model.accDetail.payload.toList, model.global).appended(Pagination.view(model))
    )
  def view(model: NftDetailModel): Html[Msg] =
    div(cls := "table-container nft")(
      model.nftDetail.activities match
        case None    => List(Head.nft, LoaderView.view)
        case Some(v) => Head.nft :: Body.nft(v.toList, model.global),
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
