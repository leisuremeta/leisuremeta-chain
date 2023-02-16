package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state
import Dom.*
import ValidOutputData.*

object TxDetailTable:
  val view = (model: Model) =>
    val data: TxDetail = TxDetailParser
      .decodeParser(model.txDetailData.get)
      .getOrElse(new TxDetail)
    genView(model, data)

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
          // onClick(PageName.TransactionDetail(data)),
          onClick(
            PageMsg.PreUpdate(
              PageName.TransactionDetail(
                // TODO :: 이렇게 하는게 맞는지 검증
                getOptionValue(Some(data), "-").toString(),
              ),
            ),
          ),
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
          // onClick(NavMsg.AccountDetail(getOptionValue(data.toAddress, "-").toString())),
          onClick(
            PageMsg.PreUpdate(
              PageName.AccountDetail(
                getOptionValue(
                  data.toAddress,
                  "-",
                )
                  .toString(),
              ),
            ),
          ),
        )(getOptionValue(data.toAddress, "-").toString()),
      ),
      div(`class` := "cell type-detail-body")(
        getOptionValue(data.value, "-").toString(),
      ),
    )
  val genOutput_NFT = (data: Transfer, i: Any) =>
    div(`class` := "row")(
      div(`class` := "cell type-detail-head")(i.toString()),
      div(`class` := "cell type-3 type-detail-body")(
        span(
          // onClick(NavMsg.AccountDetail(getOptionValue(data.toAddress, "-").toString())),
          onClick(
            PageMsg.PreUpdate(
              PageName.AccountDetail(
                getOptionValue(
                  data.toAddress,
                  "-",
                )
                  .toString(),
              ),
            ),
          ),
        )(getOptionValue(data.toAddress, "-").toString()),
      ),
      // 두개 중복되는거 같아서 하나 지움
      // div(`class` := "cell type-3 type-detail-body")(
      //   span(
      //     // onClick(NavMsg.NftDetail(data.value.toString())), // TODO :: 실데이터 받을때 이걸로 변경
      //     onClick(
      //       PageMsg.PreUpdate(
      //         PageName.AccountDetail(
      //           CommonFunc
      //             .getOptionValue(
      //               Some("26A463A0ED56A4A97D673A47C254728409C7B002"),
      //               "-",
      //             )
      //             .toString(),
      //         ),
      //       ),
      //     ),
      //   )(getOptionValue(data.value, "-").toString()),
      // ),
    )

  val genView = (model: Model, data: TxDetail) =>
    val transferHist = getOptionValue(data.transferHist, List())
      .asInstanceOf[List[Transfer]]
    val inputHashs = getOptionValue(data.inputHashs, List())
      .asInstanceOf[List[String]]

    div(`class` := "y-start gap-10px w-[100%] ")(
      div(`class` := "x")(
        div(`class` := "type-TableDetail  table-container")(
          div(`class` := "table w-[100%] ")(
            div(`class` := "row")(
              div(`class` := "cell type-detail-head ")("Transaction Hash"),
              div(`class` := "cell type-detail-body ")(
                getOptionValue(data.hash, "-").toString(),
              ),
            ),
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Created At"),
              div(`class` := "cell type-detail-body")(
                yyyy_mm_dd_time(
                  getOptionValue(data.createdAt, 0).asInstanceOf[Int],
                ),
              ),
            ),
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Signer"),
              div(
                `class` := "cell type-3 type-detail-body",
              )(
                span(
                  onClick(
                    PageMsg.PreUpdate(
                      PageName.AccountDetail(
                        getOptionValue(
                          data.signer,
                          "-",
                        )
                          .toString(),
                      ),
                    ),
                  ),
                )(getOptionValue(data.signer, "-").toString().length match
                  case 40 =>
                    getOptionValue(data.signer, "-")
                      .toString()
                      .take(10) + "..."
                  case _ =>
                    getOptionValue(data.signer, "-")
                      .toString() == "playnomm" match
                      case true =>
                        "010cd45939f064fd82403754bada713e5a9563a1"
                      case false =>
                        getOptionValue(data.signer, "-").toString(),
                ),
              ),
            ),
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Type"),
              div(`class` := "cell type-detail-body")(
                getOptionValue(data.txType, "-").toString(),
              ),
            ),
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Token Type"),
              div(`class` := "cell type-detail-body")(
                getOptionValue(data.tokenType, "-").toString(),
              ),
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
              inputHashs.length > 5 match
                case true =>
                  div(`class` := s"type-2 pt-16px")(
                    span(
                      `class` := s"${State.toggleTxDetailInput(model, ToggleMsg.ClickTxDetailInput, "_button")} ",
                      onClick(ToggleMsg.ClickTxDetailInput),
                    )("More"),
                  )
                case false => div(),
            )
              :: input(inputHashs.slice(0, 5), false),
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
                s"${getOptionValue(data.tokenType, "-").toString() == "NFT" match
                    case true  => "Token ID"
                    case false => "Value"
                  }",
              ),
            )
              :: {
                getOptionValue(data.tokenType, "-").toString() match
                  case "NFT" => output_NFT(transferHist)
                  case _     => output(transferHist)
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
              :: input(inputHashs, true),
          ),
        ),
      ),
    )
