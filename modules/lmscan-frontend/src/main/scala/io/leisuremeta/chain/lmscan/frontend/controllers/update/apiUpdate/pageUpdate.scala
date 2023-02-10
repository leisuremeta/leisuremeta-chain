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
          prevPage = model.curPage match
            case PageName.NoPage => model.prevPage
            case _               => model.curPage
          ,
          searchValueStore = search.toString(),
          pageNameStore = getPage(search),
          urlStore = search.toString(),
        ), {
          getPage(search) match
            case PageName.DashBoard =>
              Cmd.Batch(
                OnDataProcess.getData(
                  PageName.Transactions,
                  ApiPayload(page = "1"),
                ),
                OnDataProcess.getData(
                  PageName.Blocks,
                  ApiPayload(page = "1"),
                ),
                Cmd.Emit(PageMsg.PageUpdate),
              )

            case PageName.Blocks =>
              Cmd.Emit(PageMsg.PageUpdate)

            case PageName.Transactions =>
              Cmd.Emit(PageMsg.PageUpdate)

            case _ =>
              OnDataProcess.getData(
                search,
              )
        },
      )

    // #data update
    case PageMsg.DataUpdate(data: String, page: PageName) =>
      page match
        case PageName.DashBoard =>
          (
            model.copy(apiData = Some(data)),
            Cmd.None,
          )
        case PageName.Transactions =>
          log("case PageName.Transactions =>")
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
        case PageName.Blocks =>
          log("case PageName.Blocks =>")
          // TODO :: txData , tx_TotalPage 를 init 단계에서 실행되게 하는게 더 나은방법인지 생각해보자
          var updated_block_TotalPage = 1

          // TODO :: more simple code
          BlockParser
            .decodeParser(data)
            .map(data =>
              updated_block_TotalPage =
                CommonFunc.getOptionValue(data.totalPages, 1).asInstanceOf[Int],
            )

          (
            model.copy(
              blockListData = Some(data),
              block_TotalPage = updated_block_TotalPage,
            ),
            Cmd.None,
          )
        case PageName.BlockDetail(_) =>
          (
            model.copy(
              blockDetailData = Some(data),
            ),
            Cmd.emit(PageMsg.PageUpdate),
          )
        case PageName.AccountDetail(_) =>
          (
            model.copy(
              accountDetailData = Some(data),
            ),
            Cmd.emit(PageMsg.PageUpdate),
          )
        case PageName.NftDetail(_) =>
          (
            model.copy(
              nftDetailData = Some(data),
            ),
            Cmd.emit(PageMsg.PageUpdate),
          )
        case PageName.TransactionDetail(_) =>
          (
            model.copy(
              txDetailData = Some(data),
            ),
            Cmd.emit(PageMsg.PageUpdate),
          )
        case _ =>
          (
            model,
            Cmd.emit(PageMsg.GetError("페이지를 찾을수 없다..", page)),
          )

    // #page update
    case PageMsg.PageUpdate =>
      (
        model.copy(curPage = model.pageNameStore),
        Cmd.emit(PageMsg.PostUpdate),
      )

    case PageMsg.GetError(msg, page) =>
      page match
        case PageName.Page64(hash) =>
          Log.log(s"트랙잭션 먼저 검색 후 실패시 블록 디테일로 검색한다")
          (
            model,
            Cmd.emit(PageMsg.PreUpdate(PageName.BlockDetail(hash))),
          )
        case _ =>
          (
            model.copy(curPage = PageName.NoPage),
            Cmd.emit(PageMsg.PostUpdate),
          )

    case PageMsg.PostUpdate =>
      (
        model.copy(searchValue = ""),
        Cmd.None,
      )
