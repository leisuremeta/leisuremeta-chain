package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state
import scala.compiletime.ops.any
import V.*
import java.math.RoundingMode
import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.leisuremeta.chain.lmscan.common.model.AccountDetail
import scala.util.chaining.*

object AccountDetailTable:
  val view = (model: Model) =>
    // val apiData: SummaryModel = get_PageResponseViewCase(model).board
    // val data: AccountDetail   = get_PageResponseViewCase(model).accountDetail
  // val genView = (model: Model, data: AccountDetail, apiData: SummaryModel) =>
    val data = model.accDetail
    div(`class` := "y-start gap-10px w-[100%] ")(
      div(`class` := "x")(
        div(
          `class` := "type-TableDetail table-container position-relative",
        )(
          div(`class` := "m-10px w-[100%] ")(
            div()(
              div(`class` := "table w-[100%] ")(
                div(`class` := "row")(
                  gen.cell(
                    Cell.Head("Account", "cell type-detail-head"),
                    Cell.PlainStr(data.address, "cell type-detail-body"),
                  ),
                ),
                div(`class` := "row")(
                  gen.cell(
                    Cell.Head("Balance", "cell type-detail-head"),
                    Cell.Balance(
                      data.balance,
                      "cell type-detail-body",
                    ),
                  ),
                ),
                div(`class` := "row")(
                  gen.cell(
                    Cell.Head("Value", "cell type-detail-head"),
                    Cell.Any(
                      "",
                      // Pipe.accountDetailPageValue(model.lmprice, data.balance),
                      "cell type-detail-body",
                    ),
                  ),
                ),
              ),
            ),
            data != new AccountDetail match
              case false => LoaderView.view(model)
              case _     => div(`class` := "")(),
          ),
        ),
      ),
    )
