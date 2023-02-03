package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

// TODO :: A :: 페이지 매칭 방식 더 좋은 방법으로 대체하기

object NavView:
  val DashBoardPageList = List(
    NavMsg.DashBoard.toString(),
  )

  val BlockPageList =
    List(
      NavMsg.Blocks.toString(),
      NavMsg.BlockDetail.toString(),
    )

  val TransactionPageList = List(
    NavMsg.Transactions.toString(),
    NavMsg.TransactionDetail.toString(),
    NavMsg.AccountDetail.toString(),
    NavMsg.NftDetail.toString(),
  )

  def isCurPageisDashBoard = (model: Model) =>
    DashBoardPageList.contains(model.curPage.toString())

  def isCurPageisBlock = (model: Model) =>
    BlockPageList
      .reduce((a, b) => a + b)
      .contains(Log.log(model.curPage.toString().take(5))) // TODO :: A

  def isCurPageisTransaction = (model: Model) =>
    TransactionPageList
      .reduce((a, b) => a + b)
      .contains(model.curPage.toString().take(5)) // TODO :: A

  def isPrevPageisDashBoard = (model: Model) =>
    model.curPage == NavMsg.NoPage && DashBoardPageList.contains(
      model.prevPage.toString(),
    )

  def isPrevPageisBlock = (model: Model) =>
    model.curPage == NavMsg.NoPage && BlockPageList
      .reduce((a, b) => a + b)
      .contains(
        model.prevPage.toString().take(5), // TODO :: A
      )

  def isPrevPageisTransaction = (model: Model) =>
    model.curPage == NavMsg.NoPage && TransactionPageList
      .reduce((a, b) => a + b)
      .contains(
        model.prevPage.toString().take(5), // TODO :: A
      )
  def view(model: Model): Html[Msg] =
    nav(`class` := "")(
      div(id := "playnomm")("playNomm"),
      div(
        id := "buttons",
      )(
        button(
          `class` := s"${isCurPageisDashBoard(model) || isPrevPageisDashBoard(model)}",
          onClick(NavMsg.DashBoard),
        )(NavMsg.DashBoard.toString()),
        button(
          `class` := s"${isCurPageisBlock(model) || isPrevPageisBlock(model)}",
          onClick(NavMsg.Blocks),
        )(NavMsg.Blocks.toString()),
        button(
          `class` := s"${isCurPageisTransaction(model) || isPrevPageisTransaction(model)}",
          onClick(NavMsg.Transactions),
        )(NavMsg.Transactions.toString()),
      ),
    )
