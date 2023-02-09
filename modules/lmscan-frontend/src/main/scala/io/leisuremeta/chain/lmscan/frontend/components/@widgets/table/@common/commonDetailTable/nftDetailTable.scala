package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state

object NftDetailTable:
  val view = (model: Model) =>
    val data: NftDetail = NftDetailParser.decodeParser(model.nftDetailData.get).getOrElse(new NftDetail)      
    genView(model, data)

  val genView = (model: Model, data: NftDetail) =>
    val nftFile = CommonFunc.getOptionValue(data.nftFile, new NftFile).asInstanceOf[NftFile]

    div(`class` := "table-area")(
      div(id := "oop-table-blocks", `class` := "table-list x")(
        div(`class` := "x gap-32px")(
          video(`class` := "nft-image p-10px", autoPlay, loop)(
            source(src  := s"${CommonFunc.getOptionValue(nftFile.nftUri, "-").toString()}"),
          ),
          div(`class` := "y-start gap-10px w-[100%] ")(
            div()(CommonFunc.getOptionValue(nftFile.collectionName, "-").toString() + CommonFunc.getOptionValue(nftFile.nftName, "-").toString()),
            div(`class` := "x")(
              div(`class` := "type-TableDetail  table-container")(
                div(`class` := "table w-[100%] ")(
                  div(`class` := "row")(
                    div(`class` := "cell type-detail-head ")("Token ID"),
                    div(`class` := "cell type-detail-body ")(CommonFunc.getOptionValue(nftFile.tokenId, "-").toString()),
                  ),
                  div(`class` := "row")(
                    div(`class` := "cell type-detail-head")("Rarity"),
                    div(`class` := "cell type-detail-body")(CommonFunc.getOptionValue(nftFile.rarity, "-").toString()),
                  ),
                  div(`class` := "row")(
                    div(`class` := "cell type-detail-head")("Owner"),
                    div(`class` := "cell type-3 type-detail-body")(
                      span(
                        // onClick(NavMsg.AccountDetail(data.nftFile.owner)), TODO:: 실데이터 받을때 이거로 변경
                        onClick(
                          NavMsg.AccountDetail(
                            "26A463A0ED56A4A97D673A47C254728409C7B002",
                          ),
                        ),
                      )(CommonFunc.getOptionValue(nftFile.owner, "-").toString()),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    )
