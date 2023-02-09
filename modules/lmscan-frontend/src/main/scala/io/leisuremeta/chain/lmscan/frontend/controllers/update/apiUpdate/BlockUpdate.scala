package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

import Log.log

object BlockUpdate:
  def update(model: Model): BlockMsg => (Model, Cmd[IO, Msg]) =
    case BlockMsg.Refresh =>
      (model, OnBlockMsg.getBlockList(model.tx_CurrentPage.toString()))
    case BlockMsg.GetNewBlock(r) =>
      // TODO :: txData , tx_TotalPage 를 init 단계에서 실행되게 하는게 더 나은방법인지 생각해보자
      var updated_block_TotalPage = 1

      // TODO :: more simple code
      BlockParser
        .decodeParser(r)
        .map(data => updated_block_TotalPage = CommonFunc.getOptionValue(data.totalPages, 1).asInstanceOf[Int])

      (
        model.copy(
          blockListData = Some(r),
          block_TotalPage = updated_block_TotalPage,
        ),
        Cmd.None,
      )
    case BlockMsg.GetError(_) =>
      (model.copy(curPage = NavMsg.NoPage), Cmd.None)
