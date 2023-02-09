package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*
import Log.log

object PageUpdate:
  def update(model: Model): PageMsg => (Model, Cmd[IO, Msg]) =
    // [ PageMsg.PreProcess ]
    // model : prevPage , searchvaluestore 업데이트
    // cmd : searchvaluestore 기준으로 어떤 데이터를 업데이트 할지 정한다

    // PageMsg.PreProcess(search: String)
    // => onPreProcess(string) => dataProcess :> #data, #page
    // PageMsg.DataProcess(#data,#page)
    // // => onDataProcess(string) => dataProcess :> data

    // 이전페이지, 서치값 업데이트
    case PageMsg.PreUpdate(search: String) =>
      (
        model.copy(prevPage = model.curPage, searchValueStore = search),
        CommonDataProcess.getData(search),
      )
    // case PageMsg.DataUpdateProcess(search) =>
    //   // 서치값 기준으로 데이터 조회
    //   // - pass => page update
    //   // - fail => no page
    //   // -

    //   // search.length()
    //   // search.length()

    //   // search.length() match
    //   //   case Page.accountDetail =>
    //   //     (
    //   //     )
    //   (
    //     model.copy(prevPage = model.curPage, searchValueStore = search),
    //     // Cmd.Emit(PageMsg.DataUpdate),
    //     Cmd.None,
    //   )

    // #data,#page
    case PageMsg.DataUpdate(data: String, page: String) =>
      // 서치값 기준으로 데이터 조회
      // - pass => page update
      // - fail => no page
      // -

      // search.length()
      // search.length()

      // search.length() match
      //   case Page.accountDetail =>
      //     (
      //     )
      (
        model.copy(prevPage = model.curPage),
        // Cmd.Emit(PageMsg.DataUpdate),
        Cmd.None,
      )

    // #page update
    case PageMsg.PageUpdate =>
      // (
      //   model.copy(curPage = PageName.DashBoard),
      //   Cmd.None,
      // )
      (
        model.copy(prevPage = model.curPage),
        // Cmd.Emit(PageMsg.DataUpdate),
        Cmd.None,
      )

    case PageMsg.PostUpdate =>
      (
        model.copy(curPage = PageName.DashBoard),
        Cmd.None,
      )
