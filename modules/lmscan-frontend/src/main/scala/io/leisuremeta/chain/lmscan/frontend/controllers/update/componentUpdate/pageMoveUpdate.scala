package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

import Log.log

object PageMoveUpdate:
  val pageMoveCnt = 10

  def update(model: Model): PageMoveMsg => (Model, Cmd[IO, Msg]) =
    case PageMoveMsg.Next =>
      model.curPage.toString() match
        case "Transactions" =>
          val updated = model.tx_CurrentPage + pageMoveCnt
          (
            model.copy(
              tx_CurrentPage = updated,
              tx_list_Search = updated.toString(),
            ),
            OnDataProcess.getData(
              PageName.Transactions,
              ApiPayload(page = updated.toString()),
            ),
          )
        case "Blocks" =>
          val updated = model.block_CurrentPage + pageMoveCnt
          (
            model.copy(
              block_CurrentPage = updated,
              block_list_Search = updated.toString(),
            ),
            OnDataProcess.getData(
              PageName.Blocks,
              ApiPayload(page = updated.toString()),
            ),
          )
        case _ => (model, Cmd.None)

    case PageMoveMsg.Prev =>
      model.curPage.toString() match
        case "Transactions" =>
          val updated = model.tx_CurrentPage - pageMoveCnt
          (
            model.copy(
              tx_CurrentPage = updated,
              tx_list_Search = updated.toString(),
            ),
            OnDataProcess.getData(
              PageName.Transactions,
              ApiPayload(page = updated.toString()),
            ),
          )
        case "Blocks" =>
          val updated = model.block_CurrentPage - pageMoveCnt
          (
            model.copy(
              block_CurrentPage = updated,
              block_list_Search = updated.toString(),
            ),
            OnDataProcess.getData(
              PageName.Blocks,
              ApiPayload(page = updated.toString()),
            ),
          )
        case _ => (model, Cmd.None)

    case PageMoveMsg.Get(value) =>
      model.curPage.toString() match
        case "Transactions" =>
          (
            model.copy(tx_list_Search = value),
            Cmd.None,
          )
        case "Blocks" =>
          (
            model.copy(block_list_Search = value),
            Cmd.None,
          )
        case _ => (model, Cmd.None)

    case PageMoveMsg.Patch(value) =>
      model.curPage.toString() match
        case "Transactions" =>
          val str = value match
            case "Enter" => model.tx_list_Search
            case _       => value

          val res = // filter only number like string and filter overflow pagenumber
            !str.forall(
              Character.isDigit,
            ) || str == "" || str.toInt > model.tx_TotalPage match
              case true  => model.tx_CurrentPage
              case false => str.toInt

          log(s"PageMoveMsg.Patch ${str} ${res}")
          (
            model.copy(
              tx_CurrentPage = res,
              tx_list_Search = res.toString(),
            ),
            OnDataProcess.getData(
              PageName.Transactions,
              ApiPayload(page = res.toString()),
            ),
          )
        case "Blocks" =>
          val str = value match
            case "Enter" => model.block_list_Search
            case _       => value

          val res = // filter only number like string and filter overflow pagenumber
            !str.forall(
              Character.isDigit,
            ) || str == "" || str.toInt > model.block_TotalPage match
              case true  => model.block_CurrentPage
              case false => str.toInt

          (
            model.copy(
              block_CurrentPage = res,
              block_list_Search = res.toString(),
            ),
            OnDataProcess.getData(
              PageName.Blocks,
              ApiPayload(page = res.toString()),
            ),
          )

        case _ => (model, Cmd.None)
