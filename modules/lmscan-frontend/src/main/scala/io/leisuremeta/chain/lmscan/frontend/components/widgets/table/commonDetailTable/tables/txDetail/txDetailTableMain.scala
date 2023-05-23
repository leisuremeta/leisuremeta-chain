package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.common.model.TxDetail
import io.leisuremeta.chain.lmscan.frontend.Log.log

// TODO :: 콤포넌트를 더 잘게 분리
object TxDetailTableMain:
  def view(data: TxDetail) =
    div(`class` := "x")(
      div(`class` := "type-TableDetail  table-container")(
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
              Cell.ACCOUNT_HASH_DETAIL(data.signer, "cell type-detail-body"),
            ),
          ),
          div(`class` := "row")(
            gen.cell(
              Cell.Head("Type", "cell type-detail-head"),
              Cell.PlainStr(data.txType, "cell type-detail-body"),
            ),
          ),
          // div(`class` := "row")(
          //   gen.cell(
          //     Cell.Head("Token Type", "cell type-detail-head"),
          //     Cell.PlainStr(data.tokenType, "cell type-detail-body"),
          //   ),
          // ),
          div(`class` := "row")(
            gen.cell(
              Cell.Head("SubType", "cell type-detail-head"),
              Cell.PlainStr(
                {
                  // log("서브타입")
                  // log(data.subType)
                  data.subType
                },
                "cell type-detail-body",
              ),
            ),
          ),
        ),
      ),
    )
