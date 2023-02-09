package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

import Log.log

object BlockDetailUpdate:
  def update(model: Model): BlockDetailMsg => (Model, Cmd[IO, Msg]) =
    case BlockDetailMsg.Patch(hash) =>
      OnBlockDetailMsg.getBlockDetail(hash)
      (
        model,
        Cmd.None,
      )
    case BlockDetailMsg.Update(data) =>
      (
        model.copy(
          blockDetailData = Some(data),
          curPage = PageName.BlockDetail(model.searchValue),
          searchValue = "",
        ),
        Cmd.None,
      )
    case BlockDetailMsg.GetError(_) =>
      (model.copy(curPage = PageName.NoPage, searchValue = ""), Cmd.None)
