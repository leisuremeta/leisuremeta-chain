package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state
import ValidOutputData.*
import Dom.{_hidden, timeAgo}

object BlockDetailTable:
  val view = (model: Model) =>
    val data: BlockDetail = BlockDetailParser
      .decodeParser(model.blockDetailData.get)
      .getOrElse(new BlockDetail)
    genView(model, data)

  val genView = (model: Model, data: BlockDetail) =>
    div(`class` := "type-TableDetail table-container pt-16px")(
      div(`class` := "table w-[100%]")(
        div(`class` := "row")(
          div(`class` := "cell type-detail-head ")("Block Number"),
          div(`class` := "cell type-detail-body ")(
            getOptionValue(data.number, "-").toString(),
          ),
        ),
        div(`class` := "row")(
          div(`class` := "cell type-detail-head")("Timestamp"),
          div(`class` := "cell type-detail-body")(
            Dom.yyyy_mm_dd_time(
              getOptionValue(data.timestamp, 0).asInstanceOf[Int],
            ),
          ),
        ),
        div(`class` := "row")(
          div(`class` := "cell type-detail-head")("Block hash"),
          div(`class` := "cell type-detail-body")(
            getOptionValue(data.hash, "-").toString(),
          ),
        ),
        div(`class` := "row")(
          div(`class` := "cell type-detail-head")("Parent hash"),
          div(`class` := "cell type-detail-body")(
            getOptionValue(data.parentHash, "-").toString(),
          ),
        ),
        div(`class` := "row")(
          div(`class` := "cell type-detail-head")("Transcation count"),
          div(`class` := "cell type-detail-body")(
            getOptionValue(data.txCount, "-").toString(),
          ),
        ),
      ),
    )