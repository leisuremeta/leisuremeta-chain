package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

import Log.log

object AccountDetailUpdate:
  def update(model: Model): AccountDetailMsg => (Model, Cmd[IO, Msg]) =
    case AccountDetailMsg.Patch(hash) =>
      OnAccountDetailMsg.getAcountDetail(hash)
      (
        model,
        Cmd.None,
      )
    case AccountDetailMsg.Update(data) =>
      (
        model.copy(
          accountDetailData = Some(data),
          curPage = PageName.AccountDetail(model.searchValueStore),
          searchValue = "",
        ),
        Cmd.None,
      )
    case AccountDetailMsg.GetError(_) =>
      (model.copy(curPage = PageName.NoPage, searchValue = ""), Cmd.None)
