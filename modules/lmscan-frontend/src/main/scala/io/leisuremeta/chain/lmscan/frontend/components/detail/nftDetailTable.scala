package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import V.*
import io.leisuremeta.chain.lmscan.common.model.*

object NftDetailTable:
  def view(data: NftDetail) =
    val nftFile = data.nftFile.getOrElse(NftFileModel())

    div(`class` := "table-area")(
      div(`class` := "x gap-32px")(
        gen.cell(Cell.Image(nftFile.nftUri))(0),
        div(`class` := "y-start gap-10px w-[100%] ")(
          div()(
            plainStr(nftFile.collectionName) + plainStr(nftFile.nftName),
          ),
          div(
            `class` := "app-table detail table-container position-relative",
          )(
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
                    .ACCOUNT_HASH(
                      nftFile.owner,
                      "type-detail-body",
                    ),
                ),
              ),
            ),
          ),
        ),
      ),
    )
