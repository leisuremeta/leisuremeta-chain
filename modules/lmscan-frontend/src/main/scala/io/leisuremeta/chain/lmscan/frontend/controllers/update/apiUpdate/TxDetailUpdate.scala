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
        model.copy(
          txDetailData = Some(data),
          curPage = PageName.TransactionDetail(model.searchValue),
          searchValue = "",
        ),
        Cmd.None,
      )
    case TxDetailMsg.GetError(msg) =>
      log(msg)
      (model.copy(curPage = PageName.NoPage), Cmd.None)

    case TxDetailMsg.Get_64Handle_ToBlockDetail(msg) =>
      log(msg)
      (
        model.copy(prevPage = model.curPage),
        OnBlockDetailMsg.getBlockDetail(model.searchValue),
      )
    case TxDetailMsg.Get_64Handle_ToTranSactionDetail(data) =>
      (
        model.copy(
          txDetailData = Some(data),
          curPage = PageName.TransactionDetail(model.searchValue),
          searchValue = "",
        ),
        Cmd.None,
      )
