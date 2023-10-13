package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import scala.compiletime.ops.any
import V.*
import io.leisuremeta.chain.lmscan.common.model.*

object AccountDetailTable:
  def view(model: Model) =
    val data = model.accDetail
    div(
      `class` := "type-TableDetail table-container position-relative",
    )(
      div(`class` := "app-table detail w-[100%] ")(
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
            Cell.PriceS(
              data.value,
              "cell type-detail-body",
            ),
          ),
        ),
      ),
      data != new AccountDetail match
        case false => LoaderView.view
        case _     => div(),
    )
