package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

// TODO :: A :: 페이지 매칭 방식 더 좋은 방법으로 대체하기

object NavView:
  val DashBoardPageList = List(
    PageName.DashBoard.toString(),
  )

  val BlockPageList =
    List(
      PageName.Blocks.toString(),
      PageName.BlockDetail.toString(),
    )

  val TransactionPageList = List(
    PageName.Transactions.toString(),
    PageName.TransactionDetail.toString(),
    PageName.AccountDetail.toString(),
    PageName.NftDetail.toString(),
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
    model.curPage == PageName.NoPage && DashBoardPageList.contains(
      model.prevPage.toString(),
    )

  def isPrevPageisBlock = (model: Model) =>
    model.curPage == PageName.NoPage && BlockPageList
      .reduce((a, b) => a + b)
      .contains(
        model.prevPage.toString().take(5), // TODO :: A
      )

  def isPrevPageisTransaction = (model: Model) =>
    model.curPage == PageName.NoPage && TransactionPageList
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
          onClick(PageMsg.PreUpdate(PageName.DashBoard)),
        )(PageName.DashBoard.toString()),
        button(
          `class` := s"${isCurPageisBlock(model) || isPrevPageisBlock(model)}",
          onClick(PageMsg.PreUpdate(PageName.Blocks)),
        )(PageName.Blocks.toString()),
        button(
          `class` := s"${isCurPageisTransaction(model) || isPrevPageisTransaction(model)}",
          onClick(PageMsg.PreUpdate(PageName.Transactions)),
        )(PageName.Transactions.toString()),
      ),
    )
