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
          model.copy(curPage = NavMsg.DashBoard),
          Cmd.None,
        ),
      )
    case NavMsg.Blocks =>
      log(
        (
          model.copy(curPage = NavMsg.Blocks),
          Cmd.None,
        ),
      )
    case NavMsg.BlockDetail(hash) =>
      log(
        (
          model
            .copy(curPage = NavMsg.BlockDetail(hash)),
          OnBlockDetailMsg.getBlockDetail(hash),
        ),
      )
    case NavMsg.Transactions =>
      log(
        (
          model.copy(curPage = NavMsg.Transactions),
          Cmd.None,
        ),
      )
    case NavMsg.TransactionDetail(hash) =>
      log(
        (
          model.copy(
            curPage = NavMsg.TransactionDetail(hash),
          ),
          OnTxDetailMsg.getTxDetail(hash),
        ),
      )
    case NavMsg.NoPage =>
      log(
        (
          model.copy(curPage = NavMsg.NoPage),
          Cmd.None,
        ),
      )
    case NavMsg.AccountDetail(hash) =>
      log("NavMsg.AccountDetail(hash)")
      log(
        (
          model.copy(
            curPage = NavMsg.AccountDetail(hash),
          ),
          OnAccountDetailMsg.getAcountDetail(hash),
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