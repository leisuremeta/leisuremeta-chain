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
import io.leisuremeta.chain.lmscan.common.model.NftActivity

object Table:
  def block = (model: Model) =>
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        Head.block :: Body.blocks(
          {
            // 데이터가 변경되었을때는 현재 데이터, 변경되지 않았을경우 이전데이터로 보여준다
            new BlockInfo == current_ViewCase(model).blockInfo(0) match
              case true =>
                find_ViewCase(model.pointer - 1)(model).blockInfo
              case _ =>
                current_ViewCase(model).blockInfo
          },
        ),
      ),
    )

  def txList_txtable = (model: Model) =>
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        Head.tx :: Body.txlist_txtable(
          {
            // 데이터가 변경되었을때는 현재 데이터, 변경되지 않았을경우 이전데이터로 보여준다
            new TxInfo == current_ViewCase(model).txInfo(0) match
              case true =>
                find_ViewCase(model.pointer - 1)(model).txInfo
              case _ =>
                current_ViewCase(model).txInfo
          },
        ),
      ),
    )

  def accountDetail_txtable = (model: Model) =>
    log(current_ViewCase(model).txInfo)
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        new TxInfo == current_ViewCase(model).txInfo(0) match
          case true => Head.tx :: List(div())
          case _ =>
            Head.tx :: Body.accountDetail_txtable(
              {
                // // 데이터가 변경되었을때는 현재 데이터, 변경되지 않았을경우 이전데이터로 보여준다
                // new TxInfo == current_ViewCase(model).txInfo(0) match
                //   case true =>
                //   find_ViewCase(model.pointer - 1)(model).txInfo
                // case _ =>
                current_ViewCase(model).txInfo
              },
            ),
      ),
    )
  def dashboard_txtable = (model: Model) =>
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        Head.tx_dashBoard :: Body.dashboard_txtable(
          {
            // 데이터가 변경되었을때는 현재 데이터, 변경되지 않았을경우 이전데이터로 보여준다
            new TxInfo == current_ViewCase(model).txInfo(0) match
              case true =>
                find_ViewCase(model.pointer - 1)(model).txInfo
              case _ =>
                current_ViewCase(model).txInfo
          },
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
          {
            // 데이터가 변경되었을때는 현재 데이터, 변경되지 않았을경우 이전데이터로 보여준다
            new TxInfo == current_ViewCase(model).txInfo(0) match
              case true =>
                find_ViewCase(model.pointer - 1)(model).txInfo
              case _ =>
                current_ViewCase(model).txInfo
          },
        ),
      ),
    )

  def nftDetail_txtable = (model: Model) =>
    div(`class` := "m-10px")(
      div(`class` := "table w-[100%]")(
        Head.nft :: Body.nft(
          {
            val nftList = get_PageResponseViewCase(model).nftDetail.activities
              .getOrElse(List(new NftActivity))
              .toList
            // 데이터가 변경되었을때는 현재 데이터, 변경되지 않았을경우 이전데이터로 보여준다
            List(new NftActivity)(0) == nftList(0) match
              // TODO :: 처리
              case true => nftList
              case _    => nftList
          },
        ),
      ),
    )
