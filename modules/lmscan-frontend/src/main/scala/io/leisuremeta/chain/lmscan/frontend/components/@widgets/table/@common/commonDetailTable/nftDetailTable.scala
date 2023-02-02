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

  // div(`class` := "table-area")(
  //   div(id := "oop-table-blocks", `class` := "table-list x")(
  //     BlockTable.view(model),
  //     TransactionTable.view(model),
  //   ),
  // )

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
