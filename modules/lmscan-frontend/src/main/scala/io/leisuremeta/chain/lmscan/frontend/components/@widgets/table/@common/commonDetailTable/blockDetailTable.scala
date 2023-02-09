package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state

object BlockDetailTable:
  val view = (model: Model) =>
    val data: BlockDetail = BlockDetailParser.decodeParser(model.blockDetailData.get).getOrElse(new BlockDetail)      
    genView(model, data)   

  val genView = (model: Model, data: BlockDetail) =>
    div(`class` := "type-TableDetail table-container pt-16px")(
      div(`class` := "table w-[100%]")(
        div(`class` := "row")(
          div(`class` := "cell type-detail-head ")("Block Number"),
          div(`class` := "cell type-detail-body ")(CommonFunc.getOptionValue(data.number, "-").toString()),
        ),
        div(`class` := "row")(
          div(`class` := "cell type-detail-head")("Timestamp"),
          div(`class` := "cell type-detail-body")(CommonFunc.getOptionValue(data.timestamp, "-").toString()),
        ),
        div(`class` := "row")(
          div(`class` := "cell type-detail-head")("Block hash"),
          div(`class` := "cell type-detail-body")(CommonFunc.getOptionValue(data.hash, "-").toString()),
        ),
        div(`class` := "row")(
          div(`class` := "cell type-detail-head")("Parent hash"),
          div(`class` := "cell type-detail-body")(CommonFunc.getOptionValue(data.parentHash, "-").toString()),
        ),
        div(`class` := "row")(
          div(`class` := "cell type-detail-head")("Transcation count"),
          div(`class` := "cell type-detail-body")(CommonFunc.getOptionValue(data.txCount, "-").toString()),
        ),
      ),
    )
