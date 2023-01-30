package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

import Log.log

object PageMoveUpdate:
  def update(model: Model): PageMoveMsg => (Model, Cmd[IO, Msg]) =

    case PageMoveMsg.Next =>
      model.curPage.toString() match
        case "Transactions" =>
          (model.copy(tx_CurrentPage = model.tx_CurrentPage + 1), Cmd.None)
        case "Blocks" =>
          (
            model.copy(block_CurrentPage = model.block_CurrentPage + 1),
            Cmd.None,
          )
        case _ => (model, Cmd.None)

    case PageMoveMsg.Prev =>
      model.curPage.toString() match
        case "Transactions" =>
          (model.copy(tx_CurrentPage = model.tx_CurrentPage - 1), Cmd.None)
        case "Blocks" =>
          (
            model.copy(block_CurrentPage = model.block_CurrentPage - 1),
            Cmd.None,
          )
        case _ => (model, Cmd.None)

    case PageMoveMsg.Move(value) =>
      model.curPage.toString() match
        case "Transactions" =>
          (model.copy(tx_CurrentPage = value), Cmd.None)
        case "Blocks" =>
          (
            model.copy(block_CurrentPage = value),
            Cmd.None,
          )
        case _ => (model, Cmd.None)
