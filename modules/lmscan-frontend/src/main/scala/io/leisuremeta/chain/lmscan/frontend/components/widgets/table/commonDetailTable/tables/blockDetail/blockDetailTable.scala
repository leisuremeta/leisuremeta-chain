package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state
import V.*
import Dom.{_hidden, timeAgo}
import io.leisuremeta.chain.lmscan.common.model.BlockDetail
import io.leisuremeta.chain.lmscan.frontend.Log.log
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
object BlockDetailTable:
  val view = (model: Model) =>
    val data: BlockDetail = get_PageResponseViewCase(model).blockDetail
    genView(model, data)

  val genView = (model: Model, data: BlockDetail) =>
    div(
      `class` := "type-TableDetail table-container position-relative",
    )(
      div(`class` := "m-10px w-[100%] ")(
        div()(
          div(`class` := "table w-[100%]")(
            div(`class` := "row")(
              gen.cell(
                Cell.Head("Block Number", "cell type-detail-head"),
                Cell.PlainStr(data.number, "cell type-detail-body"),
              ),
            ),
            div(`class` := "row")(
              gen.cell(
                Cell.Head("Timestamp", "cell type-detail-head"),
                Cell.DATE(data.timestamp, "cell type-detail-body"),
              ),
            ),
            div(`class` := "row")(
              gen.cell(
                Cell.Head("Block hash", "cell type-detail-head"),
                Cell.PlainStr(data.hash, "cell type-detail-body"),
              ),
            ),
            div(`class` := "row")(
              gen.cell(
                Cell.Head("Parent hash", "cell type-detail-head"),
                Cell.PlainStr(data.parentHash, "cell type-detail-body"),
              ),
            ),
            div(`class` := "row")(
              gen.cell(
                Cell.Head("Transcation count", "cell type-detail-head"),
                Cell.PlainStr(data.txCount, "cell type-detail-body"),
              ),
            ),
          ),
        ),
        data != new BlockDetail match
          case false => LoaderView.view(model)
          case _     => div(`class` := "")(),
      ),
    )
