package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state

object NftDetailTable:
  val view = (model: Model) =>
    // genView(model)
    NftDetailParser
      .decodeParser(model.nftDetailData.get)
      .map(data => genView(model, data))
      .getOrElse(div())

  // TODO :: zip 말고 더 좋은 방법?
  // val input = (data: List[String]) =>
  //   data.zipWithIndex.map { ((input, i) => genInput(input, i + 1)) }
  // val output = (data: List[Transfer]) =>
  //   data.zipWithIndex.map { case ((output), i) => genOutput(output, i + 1) }

  // val genInput = (data: String, i: Any) =>
  //   div(`class` := "row")(
  //     div(`class` := "cell type-detail-body")(i.toString()),
  //     div(`class` := "cell type-3 type-detail-body")(
  //       data,
  //     ),
  //   )
  // val genOutput = (data: Transfer, i: Any) =>
  //   div(`class` := "row")(
  //     div(`class` := "cell type-detail-head")(i.toString()),
  //     div(`class` := "cell type-3 type-detail-body")(
  //       data.toAddress,
  //     ),
  //     div(`class` := "cell type-detail-body")(
  //       data.value.toString(),
  //     ),
  //   )

  // val genView = (model: Model, data: TxDetail) =>
  val genView = (model: Model, data: NftDetail) =>
    div(`class` := "x gap-32px")(
      video(`class` := "nft-image p-10px", autoPlay, loop)(
        source(src  := s"${data.nftFile.nftUri}"),
      ),
      div(`class` := "y-start gap-10px w-[100%] ")(
        div()(
          data.nftFile.collectionName + data.nftFile.nftName,
        ),
        div(`class` := "x")(
          div(`class` := "type-TableDetail  table-container")(
            div(`class` := "table w-[100%] ")(
              div(`class` := "row")(
                div(`class` := "cell type-detail-head ")("Token ID"),
                div(`class` := "cell type-detail-body ")(
                  data.nftFile.tokenId,
                ),
              ),
              div(`class` := "row")(
                div(`class` := "cell type-detail-head")("Rarity"),
                div(`class` := "cell type-detail-body")(data.nftFile.rarity),
              ),
              div(`class` := "row")(
                div(`class` := "cell type-detail-head")("Owner"),
                div(`class` := "cell type-3 type-detail-body")(
                  data.nftFile.owner,
                ),
              ),
            ),
          ),
        ),
      ),
    )
