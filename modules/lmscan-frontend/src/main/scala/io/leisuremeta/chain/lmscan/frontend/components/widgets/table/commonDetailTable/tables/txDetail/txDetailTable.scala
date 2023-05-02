package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state
import Dom.*
import V.*
import java.math.RoundingMode
import io.leisuremeta.chain.lmscan.common.model.TxDetail
import io.leisuremeta.chain.lmscan.common.model.TransferHist
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*

// TODO :: 콤포넌트를 더 잘게 분리
// FIX ::
// - 5 개 제한 삭제
// - Modal 삭제

object TxDetailTable:
  val view = (model: Model) =>
    val data: TxDetail = get_PageResponseViewCase(model).txDetail
    genView(data)

  val output = (data: List[TransferHist]) =>
    data.zipWithIndex.map { case ((output), i) => genOutput(output, i + 1) }
  val output_NFT = (data: List[TransferHist]) =>
    data.zipWithIndex.map { case ((output), i) => genOutput_NFT(output, i + 1) }

  val genOutput = (data: TransferHist, i: Any) =>
    val formatter = java.text.NumberFormat.getNumberInstance()
    formatter.setRoundingMode(RoundingMode.FLOOR)
    formatter.setMaximumFractionDigits(18)

    val value = getOptionValue(data.value, "0").toString().toDouble / Math
      .pow(10, 18)
      .toDouble

    val formattedValue = formatter.format(value)

    div(`class` := "row")(
      div(`class` := "cell type-detail-head")(i.toString()),
      div(`class` := "cell type-3 type-detail-body")(
        span(
          //   onClick(
          //     PageMsg.PreUpdate(
          //       PageName.AccountDetail(
          //         plainStr(data.toAddress),
          //       ),
          //     ),
          //   ),
        )(getOptionValue(data.toAddress, "-").toString()),
      ),
      div(`class` := "cell type-detail-body")(
        formattedValue,
      ),
    )
  val genOutput_NFT = (data: TransferHist, i: Any) =>
    div(`class` := "row")(
      div(`class` := "cell type-detail-head")(i.toString()),
      div(`class` := "cell type-3 type-detail-body")(
        span(
          //   onClick(
          //     PageMsg.PreUpdate(
          //       PageName.AccountDetail(
          //         plainStr(data.toAddress),
          //       ),
          //     ),
          //   ),
        )(getOptionValue(data.toAddress, "-").toString()),
      ),
    )

  def genView(data: TxDetail) =
    val transferHist = getOptionValue(data.transferHist, List())
      .asInstanceOf[List[TransferHist]]
    val inputHashs = getOptionValue(data.inputHashs, List())
      .asInstanceOf[List[String]]

    div(`class` := "y-start gap-10px w-[100%] ")(
      TxDetailTableMain.view(data),
      TxDetailTableInput.view(inputHashs),
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
    )
