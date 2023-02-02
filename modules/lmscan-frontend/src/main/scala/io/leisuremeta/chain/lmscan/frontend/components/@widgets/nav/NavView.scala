package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

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
    Log.log(NavMsg.TransactionDetail.toString()),
    NavMsg.Account.toString(),
    NavMsg.NftDetail.toString(),
  )

  def isCurPageisDashBoard = (model: Model) =>
    DashBoardPageList.contains(model.curPage.toString())

  def isCurPageisBlock = (model: Model) =>
    BlockPageList
      .reduce((a, b) => a + b)
      .contains(Log.log(model.curPage.toString().take(10))) // TODO :: simplify

  def isCurPageisTransaction = (model: Model) =>
    TransactionPageList
      .reduce((a, b) => a + b)
      .contains(model.curPage.toString().take(10)) // TODO :: simplify

  def isPrevPageisDashBoard = (model: Model) =>
    model.curPage == NavMsg.NoPage && DashBoardPageList.contains(
      model.prevPage.toString(),
    )

  def isPrevPageisBlock = (model: Model) =>
    model.curPage == NavMsg.NoPage && BlockPageList.contains(
      model.prevPage.toString(),
    )

  def isPrevPageisTransaction = (model: Model) =>
    model.curPage == NavMsg.NoPage && TransactionPageList
      .reduce((a, b) => a + b)
      .contains(
        model.prevPage.toString().take(10), // TODO :: simplify
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
