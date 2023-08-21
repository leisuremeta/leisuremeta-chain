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
  def view(model: Model): Html[Msg] =
    find_current_PageCase(model) match
      case _ :Transactions =>
        div(`class` := "table-container  position-relative y-center  ")(
          div(`class` := "m-10px w-[100%] ")(
            div(`class` := "  ")(
              Table.txList_txtable(model),
              Search.search_tx(model),
            ),
            TransactionTableCommon.loader(model),
          ),
        )
      case _ :DashBoard =>
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

      // case _ :BlockDetail =>
      //   div(`class` := "table-container  position-relative y-center  ")(
      //     div(`class` := "m-10px w-[100%] ")(
      //       div(`class` := "  ")(
      //         Table.blockDetail_txtable(model),
      //       ),
      //       TransactionTableCommon.loader(model),
      //     ),
      //   )

      // case _ :AccountDetail =>
      //   div(`class` := "table-container  position-relative y-center  ")(
      //     div(`class` := "m-10px w-[100%] ")(
      //       div(`class` := "  ")(
      //         Table.accountDetail_txtable(model),
      //       ),
      //       current_ViewCase(model).txInfo(0) != new TxInfo match
      //         case false => div(`class` := "")()
      //         case _     => div(`class` := "")(),
      //     ),
      //   )

      // case _ :NftDetail =>
      //   div(`class` := "table-container  position-relative y-center  ")(
      //     div(`class` := "m-10px w-[100%] ")(
      //       div(`class` := "  ")(
      //         Table.nftDetail_txtable(model),
      //       ),
      //       new NftActivity != get_PageResponseViewCase(
      //         model,
      //       ).nftDetail.activities
      //         .getOrElse(List(new NftActivity))
      //         .toList(0) match
      //         case false => LoaderView.view(model)
      //         case _     => div(`class` := "")(),
      //     ),
      //   )
      case _ => div()
