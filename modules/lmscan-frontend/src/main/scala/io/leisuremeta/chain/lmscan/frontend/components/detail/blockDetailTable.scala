package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state
import V.*
import Dom.{timeAgo}
import io.leisuremeta.chain.lmscan.common.model.BlockDetail
object BlockDetailTable:
  def view(data: BlockDetail) =
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
        data.timestamp  match
          case None => LoaderView.view
          case _     => div(),
      ),
    )
