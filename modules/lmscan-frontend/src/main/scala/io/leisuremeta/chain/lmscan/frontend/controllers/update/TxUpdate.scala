package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

import Log.log

object TxUpdate:
  def update(model: Model): TxMsg => (Model, Cmd[IO, Msg]) =
    case TxMsg.Refresh =>
      log("ApiUpdate > update > refresh")
      (model, OnTxMsg.getTxList(model.tx_CurrentPage.toString()))
    case TxMsg.GetNewTx(r) =>
      // TODO :: txData , tx_TotalPage 를 init 단계에서 실행되게 하는게 더 나은방법인지 생각해보자
      var updated_tx_TotalPage = 1

      // TODO :: more simple code
      TxParser
        .decodeParser(r)
        .map(data => updated_tx_TotalPage = data.totalPages)

      (
        model.copy(txData = Some(r), tx_TotalPage = updated_tx_TotalPage),
        Cmd.None,
      )
    case TxMsg.GetError(_) =>
      log("리프레시 > 에러 나옴")
      log((model, Cmd.None))
