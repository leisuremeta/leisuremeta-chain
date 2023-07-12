package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state
import V.*
import io.leisuremeta.chain.lmscan.common.model.NftDetail
import io.leisuremeta.chain.lmscan.common.model.NftFileModel
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*

object NftDetailTable:
  val view = (model: Model) =>
    // TODO :: 다시보기
    val data: NftDetail = get_PageResponseViewCase(model).nftDetail
    genView(model, data)

  val genView = (model: Model, data: NftDetail) =>
    val nftFile = getOptionValue(data.nftFile, new NftFileModel)

    div(`class` := "table-area")(
      div(id := "oop-table-blocks", `class` := "table-list x")(
        div(`class` := "x gap-32px")(
          gen.cell(Cell.Image(nftFile.nftUri))(0),
          div(`class` := "y-start gap-10px w-[100%] ")(
            div()(
              plainStr(nftFile.collectionName) + plainStr(nftFile.nftName),
            ),
            div(`class` := "x")(
              div(`class` := "type-TableDetail  table-container")(
                div(`class` := "table w-[100%] ")(
                  div(`class` := "row")(
                    gen.cell(
                      Cell.Head("Token ID", "cell type-detail-head"),
                      Cell
                        .Any(plainStr(nftFile.tokenId), "cell type-detail-body"),
                    ),
                  ),
                  div(`class` := "row")(
                    gen.cell(
                      Cell.Head("Rarity", "cell type-detail-head"),
                      Cell
                        .Any(rarity(nftFile.rarity), "cell type-detail-body"),
                    ),
                  ),
                  div(`class` := "row")(
                    gen.cell(
                      Cell.Head("Owner", "cell type-detail-head"),
                      Cell
                        .ACCOUNT_HASH_DETAIL(
                          nftFile.owner,
                          "cell type-3 type-detail-body",
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
