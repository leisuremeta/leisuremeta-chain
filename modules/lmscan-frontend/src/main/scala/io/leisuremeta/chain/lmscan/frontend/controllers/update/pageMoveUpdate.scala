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
          (
            model.copy(
              tx_CurrentPage = model.tx_CurrentPage + 1,
              page_Search = s"${model.tx_CurrentPage + 1}",
            ),
            Cmd.None,
          )
        case "Blocks" =>
          (
            model.copy(
              block_CurrentPage = model.block_CurrentPage + 1,
              page_Search = s"${model.block_CurrentPage + 1}",
            ),
            Cmd.None,
          )
        case _ => (model, Cmd.None)

    case PageMoveMsg.Prev =>
      model.curPage.toString() match
        case "Transactions" =>
          (
            model.copy(
              tx_CurrentPage = model.tx_CurrentPage - 1,
              page_Search = s"${model.tx_CurrentPage - 1}",
            ),
            Cmd.None,
          )
        case "Blocks" =>
          (
            model.copy(
              block_CurrentPage = model.block_CurrentPage - 1,
              page_Search = s"${model.block_CurrentPage - 1}",
            ),
            Cmd.None,
          )
        case _ => (model, Cmd.None)

    case PageMoveMsg.Get(value) =>
      model.curPage.toString() match
        case "Transactions" =>
          (
            model.copy(page_Search = value),
            Cmd.None,
          )
        case "Blocks" =>
          (
            model.copy(page_Search = value),
            Cmd.None,
          )
        case _ => (model, Cmd.None)

    case PageMoveMsg.Patch =>
      model.curPage.toString() match
        case "Transactions" =>
          val str = model.page_Search

          val res = // filter only number like string
            !str.forall(Character.isDigit) || str == "" match
              case true  => 1
              case false => str.toInt

          log(s"PageMoveMsg.Patch ${str} ${res}")
          (
            model.copy(
              tx_CurrentPage = res,
              page_Search = res.toString(),
            ),
            Cmd.None,
          )
        case "Blocks" =>
          val str = model.page_Search

          val res = // filter only number like string
            !str.forall(Character.isDigit) || str == "" match
              case true  => 1
              case false => str.toInt

          (
            model.copy(
              block_CurrentPage = res,
              page_Search = res.toString(),
            ),
            Cmd.None,
          )

        case _ => (model, Cmd.None)
