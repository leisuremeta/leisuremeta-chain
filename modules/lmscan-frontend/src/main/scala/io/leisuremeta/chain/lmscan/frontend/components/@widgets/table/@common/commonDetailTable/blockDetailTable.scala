package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state

object BlockDetailTable:
  val view = (model: Model) =>
    BlockDetailParser
      .decodeParser(model.blockDetailData.get)
      .map(data => genView(model, data))
      .getOrElse(div())

  val genView = (model: Model, data: BlockDetail) =>
    div(`class` := "type-TableDetail table-container pt-16px")(
      div(`class` := "table w-[100%]")(
        div(`class` := "row")(
          div(`class` := "cell type-detail-head ")("Block Number"),
          div(`class` := "cell type-detail-body ")("1231231"),
        ),
        div(`class` := "row")(
          div(`class` := "cell type-detail-head")("Timestamp"),
          div(`class` := "cell type-detail-body")("yyyy-mm-dd hh:mm:ss"),
        ),
        div(`class` := "row")(
          div(`class` := "cell type-detail-head")("Block hash"),
          div(`class` := "cell type-detail-body")(
            "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
          ),
        ),
        div(`class` := "row")(
          div(`class` := "cell type-detail-head")("Parent hash"),
          div(`class` := "cell type-detail-body")(
            "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
          ),
        ),
        div(`class` := "row")(
          div(`class` := "cell type-detail-head")("Transcation count"),
          div(`class` := "cell type-detail-body")("1234"),
        ),
      ),
    )
