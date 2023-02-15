package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state
import ValidOutputData.*

object NftDetailTable:
  val view = (model: Model) =>
    // TODO :: 다시보기
    val data: NftDetail = NftDetailParser
      .decodeParser(model.nftDetailData.get)
      .getOrElse(new NftDetail)
    genView(model, data)

  val genView = (model: Model, data: NftDetail) =>
    val nftFile =
      getOptionValue(data.nftFile, new NftFile).asInstanceOf[NftFile]

    div(`class` := "table-area")(
      div(id := "oop-table-blocks", `class` := "table-list x")(
        div(`class` := "x gap-32px")(
          {
            getOptionValue(nftFile.nftUri, "-").toString().contains("img") match
              case true => // 이미지 포맷
                img(
                  `class` := "nft-image p-10px",
                  src := s"${getOptionValue(nftFile.nftUri, "-").toString()}",
                )

              case _ => // 비디오 포맷
                video(`class` := "nft-image p-10px", autoPlay, loop)(
                  source(
                    src := s"https://d2t5puzz68k49j.cloudfront.net/release/collections/BPS_S.Younghoon/NFT_ITEM/DE8BB88B-48FB-4488-88BE-7F49894727AA.mp4",
                  ),
                )
          },
          div(`class` := "y-start gap-10px w-[100%] ")(
            div()(
              getOptionValue(nftFile.collectionName, "-")
                .toString() + getOptionValue(nftFile.nftName, "-")
                .toString(),
            ),
            div(`class` := "x")(
              div(`class` := "type-TableDetail  table-container")(
                div(`class` := "table w-[100%] ")(
                  div(`class` := "row")(
                    div(`class` := "cell type-detail-head ")("Token ID"),
                    div(`class` := "cell type-detail-body ")(
                      getOptionValue(nftFile.tokenId, "-").toString(),
                    ),
                  ),
                  div(`class` := "row")(
                    div(`class` := "cell type-detail-head")("Rarity"),
                    div(`class` := "cell type-detail-body")(
                      getOptionValue(nftFile.rarity, "-").toString(),
                    ),
                  ),
                  div(`class` := "row")(
                    div(`class` := "cell type-detail-head")("Owner"),
                    div(`class` := "cell type-3 type-detail-body")(
                      span(
                        // onClick(NavMsg.AccountDetail(data.nftFile.owner)), TODO:: 실데이터 받을때 이거로 변경
                        onClick(
                          PageMsg.PreUpdate(
                            PageName.AccountDetail(
                              getOptionValue(
                                nftFile.owner, // TODO :: option 처리
                                None,
                              )
                                .toString(),
                            ),
                          ),
                        ),
                      )(
                        getOptionValue(nftFile.owner, "-").toString(),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    )
