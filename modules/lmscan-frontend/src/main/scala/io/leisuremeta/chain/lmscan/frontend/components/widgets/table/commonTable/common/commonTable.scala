package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import Dom.{_hidden, timeAgo, yyyy_mm_dd_time}

import io.leisuremeta.chain.lmscan.common.model.*

object Table:
  def block(blcList: BlcList) =
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        Head.block :: Body.blocks(blcList.payload),
      ),
    )
  def txList_txtable(txList: TxList) =
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        Head.tx :: Body.txlist_txtable_off(txList.payload)
      ),
    )

  def txList_txtable = (model: Model) =>
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        {
          model.commandMode == CommandCaseMode.Development match
            case true =>
              Head.tx2 :: Body.txlist_txtable_on(
                List()
              )
            case false =>
              Head.tx :: Body.txlist_txtable_off(
                List()
              )
        },
      ),
    )

  def accountDetail_txtable = (model: Model) =>
    // log(current_ViewCase(model).txInfo)
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        Head.tx :: Body.accountDetail_txtable(
          List()
        ),
      ),
    )
  def dashboard_txtable(txList: TxList) =
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        Head.tx_dashBoard :: Body.dashboard_txtable(txList.payload),
      ),
    )
  def observer_table = (model: Model) =>
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        // Head.observer :: Body.observer(model),
      ),
    )
  def blockDetail_txtable = (model: Model) =>
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        Head.tx :: Body.blockDetail_txtable(
          List()
          // 데이터가 변경되었을때는 현재 데이터, 변경되지 않았을경우 이전데이터로 보여준다
        ),
      ),
    )

  def nftDetail_txtable = (model: Model) =>
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        Head.nft :: Body.nft(List()),
      )
    )
