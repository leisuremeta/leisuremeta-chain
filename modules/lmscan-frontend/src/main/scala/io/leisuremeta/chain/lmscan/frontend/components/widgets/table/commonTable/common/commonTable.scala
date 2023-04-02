package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import Dom.{_hidden, timeAgo, yyyy_mm_dd_time}

import Log.*
import io.leisuremeta.chain.lmscan.common.model.BlockInfo
import io.leisuremeta.chain.lmscan.frontend.OnDataProcess.getData
import io.leisuremeta.chain.lmscan.common.model.BlockDetail
import io.leisuremeta.chain.lmscan.frontend.V.getOptionValue
import io.leisuremeta.chain.lmscan.common.model.TxInfo
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*

object Table:
  def block = (model: Model) =>
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        Head.block :: Body.blocks(
          get_ViewCase(model).blockInfo,
        ),
      ),
    )

  def txList_txtable = (model: Model) =>
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        Head.tx :: Body.txlist_txtable(
          get_ViewCase(model).txInfo,
        ),
      ),
    )

  def accountDetail_txtable = (model: Model) =>
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        Head.tx :: Body.accountDetail_txtable(
          get_ViewCase(model).txInfo,
        ),
      ),
    )
  def dashboard_txtable = (model: Model) =>
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        Head.tx_dashBoard :: Body.dashboard_txtable(
          get_ViewCase(model).txInfo,
        ),
      ),
    )
  def observer_table = (model: Model) =>
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        Head.observer :: Body.observer(model),
      ),
    )
  def blockDetail_txtable = (model: Model) =>
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        Head.tx :: Body.blockDetail_txtable(
          get_ViewCase(model).txInfo,
        ),
      ),
    )
    //   Head.tx :: Body.blockDetail_txtable(
    //     // getPubData(model).blockDetail.txs.getOrElse(new BlockDetail),

    //     //     BlockDetailParser
    //     //   .decodeParser(model.blockDetailData.get)
    //     //   .getOrElse(new BlockDetail)
    //     // getOptionValue(data.txs, List()).asInstanceOf[List[TxInfo]]

    //   //   {
    //   //     val data = BlockDetailParser
    //   // .decodeParser(model.blockDetailData.get)
    //   // .getOrElse(new BlockDetail)
    //   //   }
    //   //   getOptionValue(
    //   //     getPubData(model).blockDetail.txs
    //   //       .getOrElse(new BlockDetail),
    //   //   ).txs,
    //   //   List(),
    //   //   // data.txs, List()).asInstanceOf[List[TxInfo]],
    //   // ),
    // )

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
