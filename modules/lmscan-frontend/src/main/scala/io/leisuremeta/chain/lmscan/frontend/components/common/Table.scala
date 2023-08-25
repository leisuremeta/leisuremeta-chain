package io.leisuremeta.chain.lmscan
package frontend

import tyrian.Html.*
import tyrian.*
import common.model.*

object Table:
  def mainView(model: Model): Html[Msg] =
    div(`class` := "table-container  position-relative y-center  ")(
      div(`class` := "m-10px w-[100%] ")(
        div(
          `class` := s"table-title",
        )(
          div(
            `class` := s"type-1",
          )(span()("Latest transactions")),
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
            div(`class` := "m-10px")(
              div(`class` := "table w-[100%]")(
                Head.tx_dashBoard :: Body.dashboard_txtable(v.payload),
              ),
            ),
      ),
    )

  def view(model: Model): Html[Msg] =
    div(`class` := "table-container  position-relative y-center  ")(
      div(`class` := "m-10px w-[100%] ")(
        model.txPage.list match
          case None    => LoaderView.view
          case Some(v) => div(`class` := "m-10px")(
            div(`class` := "table w-[100%]")(
              Head.tx :: Body.txlist_txtable_off(v.payload)
            ),
          )
        ,
        Pagination.view(model.txPage)
      ),
    )
  def view(model: BlockDetail): Html[Msg] =
    div(`class` := "table-area")(
      div(`class` := "table-container  position-relative y-center")(
        div(`class` := "w-[100%] ")(
          model.txs match
            case None    => LoaderView.view
            case Some(v) => Table.view(v),
        ),
      ),
    )
  def view(model: AccountDetail): Html[Msg] =
    div(`class` := "table-area")(
      div(`class` := "table-container  position-relative y-center")(
        div(`class` := "w-[100%] ")(
          model.txHistory match
            case None    => LoaderView.view
            case Some(v) => Table.view(v),
        ),
      ),
    )
  def view(model: NftDetail): Html[Msg] =
    div(`class` := "table-area")(
      div(`class` := "table-container  position-relative y-center")(
        div(`class` := "w-[100%] ")(
          model.activities match
            case None => LoaderView.view
            case Some(v) =>
              div(`class` := "m-10px")(
                div(`class` := "table w-[100%]")(
                  Head.nft :: Body.nft(v.toList),
                ),
              ),
        ),
      ),
    )

  def block(list: Option[BlcList]) =
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        list match 
          case Some(v) => Head.block :: Body.blocks(v.payload)
          case None => List(Head.block, LoaderView.view)
      ),
    )
  def view(list: Seq[TxInfo]) =
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        Head.tx :: Body.txlist_txtable_off(list.toList)
      ),
    )
