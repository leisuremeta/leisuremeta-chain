package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state
import V.*
import Dom.{_hidden, timeAgo}
import io.leisuremeta.chain.lmscan.common.model.BlockDetail

object BlockDetailTable:
  val view = (model: Model) =>
    val data: BlockDetail = BlockDetailParser.decodeParser(model.blockDetailData.get).getOrElse(new BlockDetail)
    genView(model, data)

  val genView = (model: Model, data: BlockDetail) =>
    div(`class` := "type-TableDetail table-container pt-16px")(
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
    )
