package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.circe.syntax.*

object AccountView:
  def view(model: Model): Html[Msg] =
    val copy = model.copy(txListData = Some(AccountDetailParser.txEncodeParser(AccountDetailParser.decodeParser(model.accountDetailData.get).map(_.txHistory).getOrElse(List()))))

    div(`class` := "pb-32px")(
      div(`class` := "font-40px pt-16px font-block-detail pb-16px")(
        "Account",
      ),
      div(`class` := "x")(CommonDetailTable.view(copy)),
      div(`class` := "font-40px pt-32px font-block-detail pb-16px")(
        "Transaction History",
      ),
      CommonTableView.view(copy),
    )
