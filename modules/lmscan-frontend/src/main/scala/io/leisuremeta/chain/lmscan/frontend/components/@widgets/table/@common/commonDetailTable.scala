package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

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
      case NavMsg.TransactionDetail =>
        div(`class` := "y-start gap-10px w-[100%] ")(
          div(`class` := "x")(
            div(`class` := "type-TableDetail  table-container")(
              div(`class` := "table w-[100%] ")(
                div(`class` := "row")(
                  div(`class` := "cell type-detail-head ")("Transaction Hash"),
                  div(`class` := "cell type-detail-body ")(
                    "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
                  ),
                ),
                div(`class` := "row")(
                  div(`class` := "cell type-detail-head")("Created At"),
                  div(`class` := "cell type-detail-body")("yyyy-mm-dd hh:mm:ss"),
                ),
                div(`class` := "row")(
                  div(`class` := "cell type-detail-head")("Signer"),
                  div(`class` := "cell type-3 type-detail-body")(
                    "26A463A0ED56A4A97D673A47C254728409C7B002",
                  ),
                ),
                div(`class` := "row")(
                  div(`class` := "cell type-detail-head")("Type"),
                  div(`class` := "cell type-detail-body")(
                    "Token",
                  ),
                ),
                div(`class` := "row")(
                  div(`class` := "cell type-detail-head")("Token Type"),
                  div(`class` := "cell type-detail-body")("LM"),
                ),
              ),
            ),
          ),
          div(`class` := "x")(
            div(`class` := "type-TableDetail table-container ")(
              div(`class` := "table w-[100%]")(
                div(`class` := "row")(
                  div(`class` := "cell type-detail-head")("Input"),
                  div(`class` := "cell type-detail-body font-bold")(
                    "Transaction Hash",
                  ),
                ),
                div(`class` := "row")(
                  div(`class` := "cell type-detail-body")("1"),
                  div(`class` := "cell type-3 type-detail-body")(
                    "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
                  ),
                ),
                div(`class` := "row")(
                  div(`class` := "cell type-detail-body")("2"),
                  div(`class` := "cell type-3 type-detail-body")(
                    "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
                  ),
                ),
              ),
            ),
          ),
          div(`class` := "x")(
            div(`class` := "type-TableDetail table-container")(
              div(`class` := "table w-[100%]")(
                div(`class` := "row")(
                  div(`class` := "cell type-detail-head")("Output"),
                  div(`class` := "cell type-detail-body font-bold")(
                    "To",
                  ),
                  div(`class` := "cell type-detail-body font-bold")(
                    "Value",
                  ),
                ),
                div(`class` := "row")(
                  div(`class` := "cell type-detail-head")("1"),
                  div(`class` := "cell type-3 type-detail-body")(
                    "b775871c85faae7eb5f6bcebfd28b1e1b412235c",
                  ),
                  div(`class` := "cell type-detail-body")(
                    "123456789.12345678912345678",
                  ),
                ),
                div(`class` := "row")(
                  div(`class` := "cell type-detail-head")("2"),
                  div(`class` := "cell type-3 type-detail-body")(
                    "26A463A0ED56A4A97D673A47C254728409C7B002",
                  ),
                  div(`class` := "cell type-detail-body")(
                    "1.23456789123456789",
                  ),
                ),
              ),
            ),
          ),
          div(
            `class` := s" type-2 pt-16px",
          )(span()("More")),
          textarea(
            `id` := s"transaction-text-area",
          )("asdad"),
        )

      case _ =>
        div(`class` := "row")(
          div(`class` := "cell type-detail-head")("Transcation count"),
          div(`class` := "cell type-detail-body")("1234"),
        )

object CommonDetailTable:
  def view(model: Model): Html[Msg] =
    DetailTables.render(model)
