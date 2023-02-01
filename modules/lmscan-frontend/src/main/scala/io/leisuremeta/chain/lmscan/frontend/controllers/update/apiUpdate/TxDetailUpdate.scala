package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

import Log.log

object TxDetailUpdate:
  def update(model: Model): TxDetailMsg => (Model, Cmd[IO, Msg]) =
    case TxDetailMsg.Patch(hash) =>
      OnTxDetailMsg.getTxDetail(hash)
      (
        model,
        Cmd.None,
      )
    case TxDetailMsg.Update(data) =>
      (
        model.copy(txDetailData = Some(data)),
        Cmd.None,
      )
    case TxDetailMsg.GetError(_) =>
      log((model, Cmd.None))
