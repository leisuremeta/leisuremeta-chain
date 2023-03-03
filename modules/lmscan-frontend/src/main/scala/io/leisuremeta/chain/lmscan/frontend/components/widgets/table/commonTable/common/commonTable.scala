package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import Dom.{_hidden, timeAgo, yyyy_mm_dd_time}

import Log.*

object Table:
  def block = (model: Model) =>
    div(`class` := "table w-[100%]")(
      Head.block :: Body.block(
        DataProcess.block(model),
      ),
    )

  def nft = (model: Model) =>
    div(`class` := "table w-[100%]")(
      Head.nft :: Body.nft(DataProcess.nft(model)),
    )

  def blockDetail_txtable = (model: Model) =>
    div(`class` := "table w-[100%]")(
      Head.tx :: Body.blockDetail_txtable(DataProcess.blockDetail_tx(model)),
    )

  def accountDetail_txtable = (model: Model) =>
    div(`class` := "table w-[100%]")(
      Head.tx :: Body.accountDetail_txtable(DataProcess.acountDetail_tx(model)),
    )

  def dashboard_txtable = (model: Model) =>
    div(`class` := "table w-[100%]")(
      Head.tx_dashBoard :: Body.dashboard_txtable(
        DataProcess.dashboard_tx(model),
      ),
    )

  def txList_txtable = (model: Model) =>
    div(`class` := "table w-[100%]")(
      Head.tx :: Body.txlist_txtable(
        DataProcess.dashboard_tx(model),
      ),
    )
