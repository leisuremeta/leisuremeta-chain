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

object PageUpdate:
  def update(model: Model): PageMsg => (Model, Cmd[IO, Msg]) =
    // [ PageMsg.PreProcess ]
    // model : prevPage , searchvaluestore 업데이트
    // cmd : searchvaluestore 기준으로 어떤 데이터를 업데이트 할지 정한다

    // PageMsg.PreProcess(search: String)
    // => onPreProcess(string) => dataProcess :> #data, #page
    // PageMsg.DataProcess(#data,#page)
    // // => onDataProcess(string) => dataProcess :> data

    case PageMsg.PreProcess(search: String) =>
      (
        model.copy(prevPage = model.curPage, searchValueStore = search),
        Cmd.Emit(PageMsg.DataUpdate),
      )

    case PageMsg.DataUpdate(search: String) =>
      // 서치값 기준으로 데이터 조회
      // - pass => page update
      // - fail => no page
      // -

      // search.length()
      // search.length()

      search.length() match
        case Page.accountDetail =>
          (
          )

    case PageMsg.PageUpdate(data, page) =>
      (
        model.copy(curPage = NavMsg.DashBoard),
        Cmd.None,
      )

    //    page match "s" -> (
    //       model.copy(curPage = NavMsg.DashBoard ,{page match
    //         case a -> 1
    //       } ),
    //       Cmd.None,
    //     ),
    // log(
    //   (
    //     model.copy(curPage = NavMsg.DashBoard ,{page match
    //       case a -> 1
    //     } ),
    //     Cmd.None,
    //   ),
    // )

    // 3
    case PageMsg.PageUpdateProcess =>
      log(
        (
          model.copy(curPage = NavMsg.DashBoard),
          Cmd.None,
        ),
      )

    // 4
    case PageMsg.PostProcess =>
      log(
        (
          model.copy(curPage = NavMsg.DashBoard),
          Cmd.None,
        ),
      )
