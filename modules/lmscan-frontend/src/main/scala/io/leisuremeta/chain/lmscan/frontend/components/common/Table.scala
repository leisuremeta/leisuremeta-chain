package io.leisuremeta.chain.lmscan
package frontend

import tyrian.Html.*
import tyrian.*
import common.model.*

object Table:
  def mainView(model: Model): Html[Msg] =
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
              onClick(
                RouterMsg.NavigateTo(BlockPage(1)),
              ),
            )("More"),
          ),
        ),
        block(model.blcPage.list),
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
              onClick(
                RouterMsg.NavigateTo(TxPage(1)),
              ),
            )("More"),
          ),
        ),
        model.txPage.list match
          case None => LoaderView.view
          case Some(v) =>
            div(`class` := "app-table")(
              Head.tx_dashBoard :: Body.dashboard_txtable(v.payload),
            ),
      ),
    )

  def view(model: BlockModel) =
    div(`class` := "table-container position-relative y-center")(
      Table.block(model.list),
      Pagination.view(model),
      model.list match
        case None    => LoaderView.view
        case Some(_) => div(),
    )
  def view(model: NftModel) =
    div(`class` := "table-container position-relative app-table nfts")(
      Table.nft(model.list),
      Pagination.view(model),
      model.list match
        case None    => LoaderView.view
        case Some(_) => div(),
    )
  def view(model: NftTokenModel) =
    div(`class` := "table-container position-relative app-table nft-toekn token")(
      Table.nftToken(model.list),
      Pagination.view(model),
      model.list match
        case None    => LoaderView.view
        case Some(_) => div(),
    )

  def view(model: TxModel): Html[Msg] =
    div(`class` := "table-container app-table tx w-[100%]")(
      model.list match
        case None => LoaderView.view
        case Some(v) =>
          div(`class` := "")(
            Head.tx :: Body.txlist_txtable_off(v.payload),
          )
      ,
      Pagination.view(model),
    )
  def view(model: BlockDetail): Html[Msg] =
    div(`class` := "table-container app-table mt-15 tx w-[100%]")(
      model.txs match
        case None    => List(LoaderView.view)
        case Some(v) => Table.view(v),
    )
  def view(model: AccountDetail): Html[Msg] =
    div(`class` := "table-container app-table tx w-[100%]")(
      model.txHistory match
        case None    => List(LoaderView.view)
        case Some(v) => Table.view(v),
    )
  def view(model: NftDetail): Html[Msg] =
    div(`class` := "table-container app-table nft w-[100%]")(
      model.activities match
        case None    => List(Head.nft, LoaderView.view)
        case Some(v) => Head.nft :: Body.nft(v.toList),
    )

  def block(list: Option[BlcList]) =
    div(`class` := "app-table blc w-[100%]")(
      list match
        case Some(v) => Head.block :: Body.blocks(v.payload)
        case None    => List(Head.block),
    )
  def nft(list: Option[NftList]) =
    div(`class` := "app-table blc w-[100%]")(
      list match
        case Some(v) => Head.nfts :: Body.nfts(v.payload)
        case None    => List(Head.nfts),
    )
  def nftToken(list: Option[NftTokenList]) =
    div(`class` := "app-table blc w-[100%]")(
      list match
        case Some(v) => Head.nftToken :: Body.nftToken(v.payload)
        case None    => List(Head.nftToken),
    )
  def view(list: Seq[TxInfo]) =
    Head.tx :: Body.txlist_txtable_off(list.toList)
