package io.leisuremeta.chain.lmscan
package frontend

import tyrian.Html.*
import tyrian.*
import io.circe.*, io.circe.parser.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.leisuremeta.chain.lmscan.common.model.*
import common.model.BlockDetail

object TransactionTableCommon:
  def loader = div()

object TransactionTable:
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
                RouterMsg.NavigateTo(TxPage)
              ),
            )("More"),
          ),
        ),
        Table.dashboard_txtable(model.mainPage.tList),
        model.mainPage.tList.totalCount match
          case None => LoaderView.view
          case Some(_) => div(),
      ),
    )

  def view(model: Model): Html[Msg] =
        div(`class` := "table-container  position-relative y-center  ")(
          div(`class` := "m-10px w-[100%] ")(
            div(`class` := "  ")(
              Table.txList_txtable(model.txPage.list),
              Search.view(model.txPage),
            ),
            model.txPage.list.totalCount match
              case None => LoaderView.view
              case Some(_) => div()
          ),
        )
  def view(model: BlockDetail): Html[Msg] =
        div(`class` := "table-area")(
          div(`class` := "table-container  position-relative y-center")(
            div(`class` := "w-[100%] ")(
              model.txs match
                case None => div()
                case Some(v) => Table.view(v)
              ,
              model.txs match
                case None => LoaderView.view
                case Some(_) => div()
            ),
          )
        )
  def view(model: AccountDetail): Html[Msg] =
        div(`class` := "table-area")(
          div(`class` := "table-container  position-relative y-center")(
            div(`class` := "w-[100%] ")(
              model.txHistory match
                case None => div()
                case Some(v) => Table.view(v)
              ,
              model.txHistory match
                case None => LoaderView.view
                case Some(_) => div()
            ),
          )
        )
