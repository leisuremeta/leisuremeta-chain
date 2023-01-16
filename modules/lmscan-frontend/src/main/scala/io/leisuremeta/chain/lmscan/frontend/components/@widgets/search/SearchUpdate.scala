package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

import Log.log

object InputUpdate:
  def update(model: Model): InputMsg => (Model, Cmd[IO, Msg]) =
    case InputMsg.Get(s) =>
      (model.copy(searchValue = s), Cmd.None)
    case InputMsg.Patch =>
      log("패치했을때의", model.searchValue)

      val updated =
        model.copy(
          searchValue = "",
          tab = model.searchValue match
            case "1" => NavMsg.DashBoard
            case "2" => NavMsg.Blocks
            case "3" => NavMsg.Transactions
            case _   => log("none"); NavMsg.DashBoard,
        )

      (updated, Cmd.None)
