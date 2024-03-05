package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.common.model.*

object AccountDetailTable:
  def view(data: AccountDetail) =
    div(
      cls := "detail table-container",
    )(
      div(cls := "row")(
        gen.cell(
          Cell.Head("Account", "cell type-detail-head"),
          Cell.PlainStr(data.address, "cell type-detail-body"),
        ),
      ),
      div(cls := "row")(
        gen.cell(
          Cell.Head("Balance", "cell type-detail-head"),
          Cell.Balance(
            data.balance,
            "cell type-detail-body",
          ),
        ),
      ),
      div(cls := "row")(
        gen.cell(
          Cell.Head("Value", "cell type-detail-head"),
          Cell.PriceS(
            data.value,
            "cell type-detail-body",
          ),
        ),
      ),
    )
