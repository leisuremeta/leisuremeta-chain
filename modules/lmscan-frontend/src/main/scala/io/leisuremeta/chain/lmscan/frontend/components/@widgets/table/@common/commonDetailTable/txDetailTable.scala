package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state
import Dom.*

object TxDetailTable:
  val view = (model: Model) =>
    TxDetailParser
      .decodeParser(model.txDetailData.get)
      .map(data => genView(model, data))
      .getOrElse(div())

  // TODO :: zip 말고 더 좋은 방법?
  val input = (data: List[String], isModal: Boolean) =>
    isModal match
      case true =>
        data.zipWithIndex.map { ((input, i) => genInputForModal(input, i + 1)) }
      case false =>
        data.zipWithIndex.map { ((input, i) => genInput(input, i + 1)) }

  val output = (data: List[Transfer]) =>
    data.zipWithIndex.map { case ((output), i) => genOutput(output, i + 1) }
  val output_NFT = (data: List[Transfer]) =>
    data.zipWithIndex.map { case ((output), i) => genOutput_NFT(output, i + 1) }

  val genInput = (data: String, i: Any) =>
    div(`class` := "row")(
      div(`class` := "cell type-detail-body")(i.toString()),
      div(`class` := "cell type-3 type-detail-body")(
        span(
          onClick(NavMsg.TransactionDetail(data)),
        )(data),
      ),
    )

  val genInputForModal = (data: String, i: Any) =>
    div(`class` := "row")(
      div(`class` := "cell type-detail-body")(i.toString()),
      div(`class` := "cell type-detail-body")(data),
    )

  val genOutput = (data: Transfer, i: Any) =>
    div(`class` := "row")(
      div(`class` := "cell type-detail-head")(i.toString()),
      div(`class` := "cell type-3 type-detail-body")(
        span(
          // onClick(NavMsg.AccountDetail(data.toAddress)), // TODO :: 실데이터 받을때 이걸로 변경
          onClick(
            NavMsg.AccountDetail("26A463A0ED56A4A97D673A47C254728409C7B002"),
          ),
        )(data.toAddress),
      ),
      div(`class` := "cell type-detail-body")(
        data.value.toString(),
      ),
    )
  val genOutput_NFT = (data: Transfer, i: Any) =>
    div(`class` := "row")(
      div(`class` := "cell type-detail-head")(i.toString()),
      div(`class` := "cell type-3 type-detail-body")(
        span(
          // onClick(NavMsg.AccountDetail(data.toAddress)),
          onClick(
            NavMsg.AccountDetail("26A463A0ED56A4A97D673A47C254728409C7B002"),
          ),
        )(data.toAddress),
      ),
      div(`class` := "cell type-3 type-detail-body")(
        span(
          // onClick(NavMsg.NftDetail(data.value.toString())), // TODO :: 실데이터 받을때 이걸로 변경
          onClick(NavMsg.NftDetail("2022122110000930000002558")),
        )(data.value.toString()),
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
              div(
                `class` := "cell type-3 type-detail-body",
              )(
                span(
                  onClick(NavMsg.AccountDetail(data.signer)),
                )(data.signer),
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
              data.inputHashs.length > 5 match
                case true =>
                  div(`class` := s"type-2 pt-16px")(
                    span(
                      `class` := s"${State.toggleTxDetailInput(model, ToggleMsg.ClickTxDetailInput, "_button")} ",
                      onClick(ToggleMsg.ClickTxDetailInput),
                    )("More"),
                  )
                case false => div(),
            )
              :: input(data.inputHashs.slice(0, 5), false),
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
                s"${data.tokenType == "NFT" match
                    case true  => "Token ID"
                    case false => "Value"
                  }",
              ),
            )
              :: {
                data.tokenType match
                  case "NFT" => output_NFT(data.transferHist)
                  case _     => output(data.transferHist)
              },
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
      div(
        `class` := s"${State.toggleTxDetailInput(model, ToggleMsg.ClickTxDetailInput, "_table")}",
      )(
        div(`class` := "type-TableDetail table-container txDetailModalTable")(
          div(`class` := "table w-[100%]")(
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Input"),
              div(`class` := "cell type-detail-body font-bold")(
                "Transaction Hash",
              ),
              div(`class` := s"type-2 pt-16px")(
                span(onClick(ToggleMsg.ClickTxDetailInput))("Close"),
              ),
            )
              :: input(data.inputHashs, true),
          ),
        ),
      ),
    )
