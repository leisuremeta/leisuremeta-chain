package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

import Log.log

object BlockUpdate:
  def update(model: Model): BlockMsg => (Model, Cmd[IO, Msg]) =
    case BlockMsg.Refresh =>
      log("ApiUpdate > update > refresh")
      (model, OnBlockMsg.getBlockList(model.tx_CurrentPage.toString()))
    case BlockMsg.GetNewBlock(r) =>
      // TODO :: txData , tx_TotalPage 를 init 단계에서 실행되게 하는게 더 나은방법인지 생각해보자
      var updated_block_TotalPage = 1

      // TODO :: more simple code
      BlockParser
        .decodeParser(r)
        .map(data => updated_block_TotalPage = data.totalPages)

      (
        model.copy(
          blockData = Some(r),
          block_TotalPage = updated_block_TotalPage,
        ),
        Cmd.None,
      )
    case BlockMsg.GetError(_) =>
      log("리프레시 > 에러 나옴")
      log((model, Cmd.None))
