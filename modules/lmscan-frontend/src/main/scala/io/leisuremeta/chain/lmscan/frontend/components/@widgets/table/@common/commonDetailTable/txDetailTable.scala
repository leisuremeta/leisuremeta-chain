package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state

object TxDetailTable:
  val view = (model: Model) =>
    TxDetailParser
      .decodeParser(model.txDetailData.get)
      .map(data => genView(model, data))
      .getOrElse(div())

  // TODO :: zip 말고 더 좋은 방법?
  val input = (data: List[String]) =>
    data.zipWithIndex.map { ((input, i) => genInput(input, i + 1)) }
  val output = (data: List[Transfer]) =>
    data.zipWithIndex.map { case ((output), i) => genOutput(output, i + 1) }

  // TODO :: add click event
  val genInput = (data: String, i: Any) =>
    div(`class` := "row")(
      div(`class` := "cell type-detail-body")(i.toString()),
      div(`class` := "cell type-3 type-detail-body")(
        data,
      ),
    )
  val genOutput = (data: Transfer, i: Any) =>
    div(`class` := "row")(
      div(`class` := "cell type-detail-head")(i.toString()),
      div(`class` := "cell type-3 type-detail-body")(
        data.toAddress,
      ),
      div(`class` := "cell type-detail-body")(
        data.value.toString(),
      ),
    )

  val genView = (model: Model, data: TxDetail) =>
    div(`class` := "y-start gap-10px w-[100%] ")(
      div(`class` := "x")(
        div(`class` := "type-TableDetail  table-container")(
          div(`class` := "table w-[100%] ")(
            div(`class` := "row")(
              div(`class` := "cell type-detail-head ")("Transaction Hash"),
              div(`class` := "cell type-detail-body ")(
                data.hash,
              ),
            ),
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Created At"),
              div(`class` := "cell type-detail-body")(data.createdAt.toString()),
            ),
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Signer"),
              div(`class` := "cell type-3 type-detail-body")(
                data.signer,
              ),
            ),
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Type"),
              div(`class` := "cell type-detail-body")(
                data.txType,
              ),
            ),
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Token Type"),
              div(`class` := "cell type-detail-body")(data.tokenType),
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
            )
              :: input(Log.log(data.inputHashs)),
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
            )
              :: output(data.transferHist),
          ),
        ),
      ),
      div(
        `class` := s"type-2 pt-16px",
      )(
        span(
          `class` := s"${State.toggle(model, ToggleMsg.Click, "_button")} ",
          onClick(ToggleMsg.Click),
        )("More"),
      ),
      div(`class` := "pt-12px x-center")(
        textarea(
          `id`    := s"transaction-text-area",
          `class` := s"${State.toggle(model, ToggleMsg.Click, "_textarea")}",
        )(s"${TxDetailParser.txDetailEncoder(data)}"),
      ),
    )
