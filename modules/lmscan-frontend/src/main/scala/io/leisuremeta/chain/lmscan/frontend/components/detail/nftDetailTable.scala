package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.common.model.*

object NftDetailTable:
  def view(data: NftDetail) =
    val nftFile = data.nftFile.getOrElse(NftFileModel())

    div(cls := "nft-detail")(
      gen.cell(Cell.Image(nftFile.nftUri))(0),
      div(cls := "detail table-container")(
        div(cls := "row")(
          span("NFT Name"),
          span(nftFile.nftName.getOrElse("-")),
        ),
        div(cls := "row")(
          span("Collection Name"),
          span(nftFile.collectionName.getOrElse("-")),
        ),
        div(cls := "row")(
          span("Token ID"),
          span(nftFile.tokenId.getOrElse("-")),
        ),
        div(cls := "row")(
          span("Definition ID"),
          span(nftFile.tokenDefId.getOrElse("-")),
        ),
        div(cls := "row")(
          span("Rarity"),
          span(nftFile.rarity.getOrElse("-")),
        ),
        div(cls := "row")(
          span("Creator"),
          span(nftFile.creator.getOrElse("-")),
        ),
        div(cls := "row")(
          span("Owner"),
          ParseHtml.fromAccHash(nftFile.owner),
        ),
        div(cls := "row")(
          span("Issue Date"),
          ParseHtml.fromDate(nftFile.createdAt),
        ),
      ),
    )
