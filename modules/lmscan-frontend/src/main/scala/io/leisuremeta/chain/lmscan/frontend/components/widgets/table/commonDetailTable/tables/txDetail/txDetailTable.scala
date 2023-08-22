package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import Dom.*
import V.*
import java.math.RoundingMode
import io.leisuremeta.chain.lmscan.common.model.TxDetail
import io.leisuremeta.chain.lmscan.common.model.TransferHist

object TxDetailTable:
  def view(model: Model) =
    // val data: TxDetail = get_PageResponseViewCase(model).txDetail
    div(`class` := "y-start gap-10px w-[100%] ")(
      // TxDetailTableMain.view(data) :: TxDetailTableCommon.view(data),
    )
