package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*
import Log.log
import CustomMap.*

object PageUpdate:
  def update(model: Model): PageMsg => (Model, Cmd[IO, Msg]) =
    case PageMsg.PreUpdate(search: PageName) =>
      (
        model.copy(
          prevPage = model.curPage,
          searchValueStore = search.toString(),
          pageNameStore = getPage(search),
          urlStore = search.toString(),
        ), {
          List(PageName.DashBoard, PageName.Blocks, PageName.Transactions)
            .contains(getPage(search)) match
            case true => // 단순 버튼 => 즉시 페이지 변경
              Cmd.Emit(PageMsg.PageUpdate)
            case false => // api 데이터 호출 => 데이터 프로세스 먼저 실행 후 페이지 변경
              OnDataProcess.getData(
                search,
              )
        },
      )

    // #data update
    case PageMsg.DataUpdate(data: String) =>
      model.pageNameStore match
        case PageName.DashBoard =>
          (
            model.copy(apiData = Some(data)),
            Cmd.None,
          )
        case PageName.Transactions =>
          // TODO :: txData , tx_TotalPage 를 init 단계에서 실행되게 하는게 더 나은방법인지 생각해보자
          var updated_tx_TotalPage = 1

          // TODO :: more simple code
          TxParser
            .decodeParser(data)
            .map(data =>
              updated_tx_TotalPage =
                CommonFunc.getOptionValue(data.totalPages, 1).asInstanceOf[Int],
            )

          (
            model.copy(
              txListData = Some(data),
              tx_TotalPage = updated_tx_TotalPage,
            ),
            Cmd.None,
          )
        case _ =>
          (
            model.copy(curPage = model.pageNameStore),
            Cmd.None,
          )

    // #page update
    case PageMsg.PageUpdate =>
      (
        model.copy(curPage = model.pageNameStore),
        Cmd.None,
      )

    case PageMsg.PostUpdate =>
      (
        model.copy(curPage = PageName.DashBoard),
        Cmd.None,
      )

    case PageMsg.GetError(msg) =>
      Log.log(msg)
      (
        model.copy(curPage = PageName.NoPage),
        Cmd.None,
      )
