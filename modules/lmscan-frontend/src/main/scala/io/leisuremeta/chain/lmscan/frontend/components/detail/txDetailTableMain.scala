package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.common.model.TxDetail

// TODO :: 콤포넌트를 더 잘게 분리
object TxDetailTableMain:
  def view(data: TxDetail) =
    div(`class` := "app-table detail table-container position-relative")(
      div(`class` := "table w-[100%] ")(
        div(`class` := "row")(
          gen.cell(
            Cell.Head("Transaction Hash", "cell type-detail-head"),
            Cell.PlainStr(data.hash, "cell type-detail-body"),
          ),
        ),
        div(`class` := "row")(
          gen.cell(
            Cell.Head("Created At", "cell type-detail-head"),
            Cell.DATE(data.createdAt, "cell type-detail-body"),
          ),
        ),
        div(`class` := "row")(
          gen.cell(
            Cell.Head("Signer", "cell type-detail-head"),
            Cell.ACCOUNT_HASH(data.signer, "type-detail-body"),
          ),
        ),
        div(`class` := "row")(
          gen.cell(
            Cell.Head("Type", "cell type-detail-head"),
            Cell.PlainStr(data.txType, "cell type-detail-body"),
          ),
        ),
        div(`class` := "row")(
          gen.cell(
            Cell.Head("SubType", "cell type-detail-head"),
            Cell.PlainStr(
              data.subType,
              "cell type-detail-body",
            ),
          ),
        ),
      ),
    )
