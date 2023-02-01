package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state

object DetailTables:
  def render(model: Model): Html[Msg] =
    model.curPage match
      case NavMsg.BlockDetail =>
        div(`class` := "type-TableDetail table-container pt-16px")(
          div(`class` := "table w-[100%]")(
            div(`class` := "row")(
              div(`class` := "cell type-detail-head ")("Block Number"),
              div(`class` := "cell type-detail-body ")("1231231"),
            ),
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Timestamp"),
              div(`class` := "cell type-detail-body")("yyyy-mm-dd hh:mm:ss"),
            ),
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Block hash"),
              div(`class` := "cell type-detail-body")(
                "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
              ),
            ),
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Parent hash"),
              div(`class` := "cell type-detail-body")(
                "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
              ),
            ),
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Transcation count"),
              div(`class` := "cell type-detail-body")("1234"),
            ),
          ),
        )
      case NavMsg.TransactionDetail(_) =>
        TxDetailTable.view(model)

      case NavMsg.Account =>
        div(`class` := "y-start gap-10px w-[100%] ")(
          div(`class` := "x")(
            div(`class` := "type-TableDetail  table-container")(
              div(`class` := "table w-[100%] ")(
                div(`class` := "row")(
                  div(`class` := "cell type-detail-head ")("Account"),
                  div(`class` := "cell type-detail-body ")(
                    "b775871c85faae7eb5f6bcebfd28b1e1b412235c",
                  ),
                ),
                div(`class` := "row")(
                  div(`class` := "cell type-detail-head")("Balance"),
                  div(`class` := "cell type-detail-body")("1.23456789 LM"),
                ),
                div(`class` := "row")(
                  div(`class` := "cell type-detail-head")("Value"),
                  div(`class` := "cell type-3 type-detail-body")(
                    "$123.4567",
                  ),
                ),
              ),
            ),
          ),
        )

      case NavMsg.Nft =>
        div(`class` := "y-start gap-10px w-[100%] ")(
          div()("Light in the Shadows #1"),
          div(`class` := "x")(
            div(`class` := "type-TableDetail  table-container")(
              div(`class` := "table w-[100%] ")(
                div(`class` := "row")(
                  div(`class` := "cell type-detail-head ")("Token ID"),
                  div(`class` := "cell type-detail-body ")(
                    "2022122110000930000002558",
                  ),
                ),
                div(`class` := "row")(
                  div(`class` := "cell type-detail-head")("Rarity"),
                  div(`class` := "cell type-detail-body")("UNIQ"),
                ),
                div(`class` := "row")(
                  div(`class` := "cell type-detail-head")("Owner"),
                  div(`class` := "cell type-3 type-detail-body")(
                    "b775871c85faae7eb5f6bcebfd28b1e1b412235c",
                  ),
                ),
              ),
            ),
          ),
        )

      case _ =>
        div(`class` := "row")(
          div(`class` := "cell type-detail-head")("Transcation count"),
          div(`class` := "cell type-detail-body")("1234"),
        )

object CommonDetailTable:
  def view(model: Model): Html[Msg] =
    DetailTables.render(model)
