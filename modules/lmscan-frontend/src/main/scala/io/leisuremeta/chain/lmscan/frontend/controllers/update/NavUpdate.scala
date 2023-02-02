package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*
import Log.log

object NavUpdate:
  def update(model: Model): NavMsg => (Model, Cmd[IO, Msg]) =
    case NavMsg.DashBoard =>
      log(
        (
          model.copy(curPage = NavMsg.DashBoard, prevPage = model.curPage),
          Cmd.None,
        ),
      )
    case NavMsg.Blocks =>
      log(
        (
          model.copy(curPage = NavMsg.Blocks, prevPage = model.curPage),
          Cmd.None,
        ),
      )
    case NavMsg.BlockDetail(hash) =>
      log(
        (
          model
            .copy(curPage = NavMsg.BlockDetail(hash), prevPage = model.curPage),
          OnBlockDetailMsg.getBlockDetail(hash),
        ),
      )
    case NavMsg.Transactions =>
      log(
        (
          model.copy(curPage = NavMsg.Transactions, prevPage = model.curPage),
          Cmd.None,
        ),
      )
    case NavMsg.TransactionDetail(hash) =>
      log(
        (
          model.copy(
            curPage = NavMsg.TransactionDetail(hash),
            prevPage = model.prevPage,
          ),
          OnTxDetailMsg.getTxDetail(hash),
        ),
      )
    case NavMsg.NoPage =>
      log(
        (
          model.copy(curPage = NavMsg.NoPage, prevPage = model.prevPage),
          Cmd.None,
        ),
      )
    case NavMsg.Account =>
      log(
        (
          model.copy(curPage = NavMsg.Account, prevPage = model.prevPage),
          Cmd.None,
        ),
      )
    case NavMsg.NftDetail(hash) =>
      log(
        (
          model
            .copy(curPage = NavMsg.NftDetail(hash), prevPage = model.prevPage),
          OnNftDetailMsg.getNftDetail(hash),
        ),
      )
