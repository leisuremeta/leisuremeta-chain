package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import Dom.{_hidden, timeAgo, yyyy_mm_dd_time}

import Log.*
import io.leisuremeta.chain.lmscan.frontend.Builder.*
import io.leisuremeta.chain.lmscan.common.model.BlockInfo

object Table:
  def block = (model: Model) =>
    div(`class` := "table w-[100%]")(
      Head.block :: Body.block(
        // Builder.get_Pub_M3(model.observers).blockInfo,
        List(new BlockInfo),
      ),
    )
  def txList_txtable = (model: Model) =>
    div(`class` := "table w-[100%]")(
      // Head.tx :: Body.txlist_txtable(
      //   DataProcess.dashboard_tx(model),
      // ),
    )
  def observer_table = (model: Model) =>
    div(`class` := "table w-[100%]")(
      Head.observer :: Body.observer(model),
    )

//   def dashboard_txtable = (model: Model) =>
//     div(`class` := "table w-[100%]")(
//       Head.tx_dashBoard :: Body.dashboard_txtable(
//         DataProcess.dashboard_tx(model),
//       ),
//     )

//   def nftDetail_txtable = (model: Model) =>
//     div(`class` := "table w-[100%]")(
//       Head.nft :: Body.nft(DataProcess.nft(model)),
//     )

//   def blockDetail_txtable = (model: Model) =>
//     div(`class` := "table w-[100%]")(
//       Head.tx :: Body.blockDetail_txtable(DataProcess.blockDetail_tx(model)),
//     )

//   def accountDetail_txtable = (model: Model) =>
//     div(`class` := "table w-[100%]")(
//       Head.tx :: Body.accountDetail_txtable(DataProcess.acountDetail_tx(model)),
//     )
