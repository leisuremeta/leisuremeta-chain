package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.common.model.BlockDetail
object BlockDetailTable:
  def view(data: BlockDetail) =
    div(
      cls := "detail table-container",
    )(
      div(cls := "row")(
        gen.cell(
          Cell.Head("Block Number", "cell type-detail-head"),
          Cell.PlainStr(data.number, "cell type-detail-body"),
        ),
      ),
      div(cls := "row")(
        gen.cell(
          Cell.Head("Timestamp", "cell type-detail-head"),
          Cell.DATE(data.timestamp, "cell type-detail-body"),
        ),
      ),
      div(cls := "row")(
        gen.cell(
          Cell.Head("Block hash", "cell type-detail-head"),
          Cell.PlainStr(data.hash, "cell type-detail-body"),
        ),
      ),
      div(cls := "row")(
        gen.cell(
          Cell.Head("Parent hash", "cell type-detail-head"),
          Cell.BLOCK_HASH(data.parentHash),
        ),
      ),
      div(cls := "row")(
        gen.cell(
          Cell.Head("Transaction count", "cell type-detail-head"),
          Cell.PlainStr(data.txCount, "cell type-detail-body"),
        ),
      ),
    )
