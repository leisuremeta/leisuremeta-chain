package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import V.*
import io.leisuremeta.chain.lmscan.common.model.*

object NftDetailTable:
  def view(data: NftDetail) =
    val nftFile = data.nftFile.getOrElse(NftFileModel())

    div(cls := "nft-detail")(
      gen.cell(Cell.Image(nftFile.nftUri))(0),
      div(cls := "nft-title")(
        plainStr(nftFile.collectionName) + plainStr(nftFile.nftName),
      ),
      div(
        cls := "detail table-container",
      )(
        div(cls := "row")(
          gen.cell(
            Cell.Head("Token ID", "cell type-detail-head"),
            Cell
              .Any(plainStr(nftFile.tokenId), "cell type-detail-body"),
          ),
        ),
        div(cls := "row")(
          gen.cell(
            Cell.Head("Rarity", "cell type-detail-head"),
            Cell
              .Any(rarity(nftFile.rarity), "cell type-detail-body"),
          ),
        ),
        div(cls := "row")(
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
    )
