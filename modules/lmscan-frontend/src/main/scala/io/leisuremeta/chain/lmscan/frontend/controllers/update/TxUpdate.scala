package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

import Log.log

object TxUpdate:
  def update(model: Model): TxMsg => (Model, Cmd[IO, Msg]) =
    case TxMsg.Refresh =>
      log("ApiUpdate > update > refresh")
      // log((model, OnTxMsg.getTxList("ran")))
      (model, OnTxMsg.getTxList(model.tx_CurrentPage.toString()))
    case TxMsg.GetNewTx(r) =>
      // log("모델에서, 새로운 url로 업데이트 하면된다")
      // log(r)
      // log((model, Cmd.None))
      (model.copy(data = Some(r)), Cmd.None)
    case TxMsg.GetError(_) =>
      log("리프레시 > 에러 나옴")
      log((model, Cmd.None))
