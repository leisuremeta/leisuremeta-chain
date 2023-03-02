package io.leisuremeta.chain.lmscan.frontend
import scala.util.chaining.*
import tyrian.Html.*
import tyrian.*
import Dom.*
import V.*
import java.math.RoundingMode
import io.leisuremeta.chain.lmscan.common.model.TxDetail
import io.leisuremeta.chain.lmscan.common.model.TransferHist
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*

object TxDetailTableOutput:

  val output = (data: List[TransferHist]) =>
    data.zipWithIndex.map { case ((output), i) => genOutput(output, i + 1) }
  val output_NFT = (data: List[TransferHist]) =>
    data.zipWithIndex.map { case ((output), i) => genOutput_NFT(output, i + 1) }

  val genOutput = (data: TransferHist, i: Any) =>
    div(`class` := "row")(
      div(`class` := "cell type-detail-head")(i.toString()),
      div(`class` := "cell type-3 type-detail-body")(
        span(
        )(getOptionValue(data.toAddress, "-").toString()),
      ),
      div(`class` := "cell type-detail-body")(
        data.value.pipe(s => Pipe.txDetailTableOutput(s)),
      ),
    )
  val genOutput_NFT = (data: TransferHist, i: Any) =>
    div(`class` := "row")(
      div(`class` := "cell type-detail-head")(i.toString()),
      div(`class` := "cell type-3 type-detail-body")(
        span(
        )(getOptionValue(data.toAddress, "-").toString()),
      ),
    )

  def view(data: TxDetail) =
    val transferHist = getOptionValue(data.transferHist, List())
      .asInstanceOf[List[TransferHist]]

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
    )
