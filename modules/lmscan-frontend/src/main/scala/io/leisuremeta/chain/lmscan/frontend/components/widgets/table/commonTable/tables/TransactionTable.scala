package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.circe.*, io.circe.parser.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import Dom.{_hidden, isEqGet, yyyy_mm_dd_time, timeAgo}
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
import Log.*
import io.leisuremeta.chain.lmscan.common.model.*

object TransactionTable:
  def view(model: Model): Html[Msg] =
    find_current_PageCase(model) match
      case PageCase.Transactions(_, _, _, _) =>
        div(`class` := "table-container  position-relative y-center  ")(
          div(`class` := "m-10px w-[100%] ")(
            div(`class` := "  ")(
              Table.txList_txtable(model),
              Search.search_tx(model),
            ),
            current_ViewCase(model).txInfo(0) != new TxInfo match
              case false => LoaderView.view(model)
              case _     => div(),
          ),
        )
      case PageCase.DashBoard(_, _, _, _) =>
        div(`class` := "table-container  position-relative y-center  ")(
          div(`class` := "m-10px w-[100%] ")(
            div(`class` := "  ")(
              Title.tx(model),
              Table.dashboard_txtable(model),
            ),
            current_ViewCase(model).txInfo(0) != new TxInfo match
              case false => LoaderView.view(model)
              case _     => div(`class` := "")(),
          ),
        )

      case PageCase.BlockDetail(_, _, _, _) =>
        div(`class` := "table-container  position-relative y-center  ")(
          div(`class` := "m-10px w-[100%] ")(
            div(`class` := "  ")(
              Table.blockDetail_txtable(model),

              // TODO :: 리스트가 1페이지 이상일때만 search 보여주기
              // Search.search_tx(model),
            ),
            current_ViewCase(model).txInfo(0) != new TxInfo match
              case false => LoaderView.view(model)
              case _     => div(`class` := "")(),
          ),
        )

      case PageCase.AccountDetail(_, _, _, _) =>
        div(`class` := "table-container  position-relative y-center  ")(
          div(`class` := "m-10px w-[100%] ")(
            div(`class` := "  ")(
              Table.accountDetail_txtable(model),

              // TODO :: 리스트가 1페이지 이상일때만 search 보여주기
              // Search.search_tx(model),
            ),
            current_ViewCase(model).txInfo(0) != new TxInfo match
              case false => LoaderView.view(model)
              case _     => div(`class` := "")(),
          ),
        )

      case PageCase.NftDetail(_, _, _, _) =>
        div(`class` := "table-container  position-relative y-center  ")(
          div(`class` := "m-10px w-[100%] ")(
            div(`class` := "  ")(
              Table.nftDetail_txtable(model),

              // TODO :: 리스트가 1페이지 이상일때만 search 보여주기
              // Search.search_tx(model),
            ),
            new NftActivity != get_PageResponseViewCase(
              model,
            ).nftDetail.activities
              .getOrElse(List(new NftActivity))
              .toList(0) match
              case false => LoaderView.view(model)
              case _     => div(`class` := "")(),
          ),
        )
      case _ => div()
      // div(`class` := "table-container")(
      //   Table.blockDetail_txtable(model),
      // )
