package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

import Log.log

object Page:
  // 해시 자릿수에 따른 페이지 렌더링
  val accountDetail: Int = 40
  val nftDetail: Int     = 25
  val handle_64: Int     = 64
  val custom: Int        = 1

object SearchUpdate:
  def update(model: Model): InputMsg => (Model, Cmd[IO, Msg]) =
    case InputMsg.Get(s) =>
      (model.copy(searchValue = s), Cmd.None)

    case InputMsg.Patch =>
      val hash = model.searchValue

      model.searchValue.length() match

        case Page.accountDetail =>
          (
            model.copy(
              prevPage = model.curPage,
            ),
            OnAccountDetailMsg.getAcountDetail(hash),
          )

        case Page.nftDetail =>
          (
            model.copy(
              prevPage = model.curPage,
            ),
            OnNftDetailMsg.getNftDetail(hash),
          )

        case Page.handle_64 =>
          (
            model,
            OnHandle_64.getData(hash),
          )

        case Page.custom =>
          model.searchValue match
            case "1" =>
              (
                model.copy(
                  searchValue = "",
                  curPage = NavMsg.DashBoard,
                  prevPage = model.curPage,
                ),
                Cmd.None,
              )
            case "2" =>
              (
                model.copy(
                  searchValue = "",
                  curPage = NavMsg.Blocks,
                  prevPage = model.curPage,
                ),
                Cmd.None,
              )
            case "3" =>
              (
                model.copy(
                  searchValue = "",
                  curPage = NavMsg.Transactions,
                  prevPage = model.curPage,
                ),
                Cmd.None,
              )
            case _ =>
              (
                model.copy(
                  searchValue = "",
                  curPage = NavMsg.NoPage,
                  prevPage = model.curPage match
                    // noPage 일때는, 이전페이지를 변경하지 않는다.
                    case NavMsg.NoPage => model.prevPage
                    case _             => model.curPage,
                ),
                Cmd.None,
              )
        case _ =>
          (
            model.copy(
              searchValue = "",
              curPage = NavMsg.NoPage,
              prevPage = model.curPage match
                // noPage 일때는, 이전페이지를 변경하지 않는다.
                case NavMsg.NoPage => model.prevPage
                case _             => model.curPage,
            ),
            Cmd.None,
          )
