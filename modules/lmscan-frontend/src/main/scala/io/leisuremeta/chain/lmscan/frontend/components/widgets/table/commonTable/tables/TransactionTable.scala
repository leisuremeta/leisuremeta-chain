package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.circe.*, io.circe.parser.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import Dom.{_hidden, isEqGet, yyyy_mm_dd_time, timeAgo}
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
import io.leisuremeta.chain.lmscan.common.model.*

object TransactionTableCommon:
  def loader(model: Model) =
    val isLoader = current_ViewCase(model).txInfo(0) != new TxInfo

    isLoader match
      case false => LoaderView.view(model)
      case _     => div()

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
              onClick(PageMsg.PreUpdate(Transactions())),
            )("More"),
          ),
        ),
        Table.dashboard_txtable(model.mainPage.txList),
        model.mainPage.txList.totalCount match
          case None => LoaderView.view
          case Some(_) => div(),
      ),
    )

  def view(model: Model): Html[Msg] =
    find_current_PageCase(model) match
      case _: Transactions =>
        div(`class` := "table-container  position-relative y-center  ")(
          div(`class` := "m-10px w-[100%] ")(
            div(`class` := "  ")(
              Table.txList_txtable(model),
              Search.search_tx(model),
            ),
            TransactionTableCommon.loader(model),
          ),
        )
      case _ => div()
