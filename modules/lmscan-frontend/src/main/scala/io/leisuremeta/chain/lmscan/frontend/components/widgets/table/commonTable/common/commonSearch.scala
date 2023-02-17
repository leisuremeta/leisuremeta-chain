package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import Dom.{_hidden, timeAgo, yyyy_mm_dd_time, _selectedPage}
// import ValidOutputData.*
// import scala.runtime.RichInt

object Search:
  val search_block = (model: Model) =>
    val curPage   = model.block_CurrentPage
    val totalPage = model.block_TotalPage

    val btnFistPage = curPage match
      case x if (x == 1 || x == 2)     => 1
      case x if (x == totalPage)       => (totalPage - 5)
      case x if (x == (totalPage - 1)) => (totalPage - 4)
      case x                           => (curPage - 2)
    val btnLastPage = btnFistPage + 5

    div(
      `class` := s"${State.curPage(model, PageName.DashBoard, "_search")} table-search xy-center ",
    )(
      div(`class` := "xy-center")(
        div(
          `class` := s"type-arrow",
          curPage == 1 match
            case true =>
              style(Style("color" -> "lightgray"))
            case false =>
              onClick(PageMoveMsg.Patch("1")),
        )("<<"),
        div(
          `class` := s"type-arrow",
          curPage <= 10 match
            case true =>
              style(Style("color" -> "lightgray"))
            case false =>
              onClick(PageMoveMsg.Prev),
        )("<"),
        div(`class` := s"type-text-btn")(
          List
            .range(btnFistPage, btnLastPage)
            .map(idx =>
              span(
                `class` := s"${_selectedPage[Int](curPage, idx)}",
                onClick(PageMoveMsg.Patch(idx.toString())),
              )(idx.toString()),
            ),
        ),
        div(
          `class` := s"type-arrow",
          curPage >= (totalPage - 10) match
            case true =>
              style(Style("color" -> "lightgray"))
            case false =>
              onClick(PageMoveMsg.Next),
        )(">"),
        div(
          `class` := s"type-arrow",
          curPage == totalPage match
            case true =>
              style(Style("color" -> "lightgray"))
            case false =>
              onClick(PageMoveMsg.Patch(totalPage.toString())),
        )(">>"),
      ),
    )
    // div(
    //   `class` := s"${State.curPage(model, PageName.DashBoard, "_search")} table-search xy-center ",
    // )(
    //   div(`class` := "xy-center")(
    //     div(
    //       `class` := s"type-arrow ${_hidden[Int](1, model.block_CurrentPage)}",
    //       onClick(PageMoveMsg.Patch("1")),
    //     )("<<"),
    //     div(
    //       `class` := s"type-arrow ${_hidden[Int](1, model.block_CurrentPage)}",
    //       onClick(PageMoveMsg.Prev),
    //     )("<"),
    //     div(`class` := "type-plain-text")("Page"),
    //     input(
    //       onInput(s => PageMoveMsg.Get(s)),
    //       value   := s"${model.block_list_Search}",
    //       `class` := "type-search xy-center DOM-page1 ",
    //     ),
    //     div(`class` := "type-plain-text")("of"),
    //     div(`class` := "type-plain-text")(model.block_TotalPage.toString()),
    //     div(
    //       `class` := s"type-arrow ${_hidden[Int](model.block_TotalPage, model.block_CurrentPage)}",
    //       onClick(PageMoveMsg.Next),
    //     )(">"),
    //     div(
    //       `class` := s"type-arrow ${_hidden[Int](model.block_TotalPage, model.block_CurrentPage)}",
    //       onClick(PageMoveMsg.Patch(model.block_TotalPage.toString())),
    //     )(">>"),
    //   ),
    // )
  val search_tx = (model: Model) =>
    val curPage   = model.tx_CurrentPage
    val totalPage = model.tx_TotalPage

    val btnFistPage = curPage match
      case x if (x == 1 || x == 2)     => 1
      case x if (x == totalPage)       => (totalPage - 5)
      case x if (x == (totalPage - 1)) => (totalPage - 4)
      case x                           => (curPage - 2)
    val btnLastPage = btnFistPage + 5

    div(
      `class` := s"${State.curPage(model, PageName.DashBoard, "_search")} table-search xy-center ",
    )(
      div(`class` := "xy-center")(
        div(
          `class` := s"type-arrow",
          curPage == 1 match
            case true =>
              style(Style("color" -> "lightgray"))
            case false =>
              onClick(PageMoveMsg.Patch("1")),
        )("<<"),
        div(
          `class` := s"type-arrow",
          curPage <= 10 match
            case true =>
              style(Style("color" -> "lightgray"))
            case false =>
              onClick(PageMoveMsg.Prev),
        )("<"),
        div(`class` := s"type-text-btn")(
          List
            .range(btnFistPage, btnLastPage)
            .map(idx =>
              span(
                `class` := s"${_selectedPage[Int](curPage, idx)}",
                onClick(PageMoveMsg.Patch(idx.toString())),
              )(idx.toString()),
            ),
        ),
        div(
          `class` := s"type-arrow",
          curPage >= (totalPage - 10) match
            case true =>
              style(Style("color" -> "lightgray"))
            case false =>
              onClick(PageMoveMsg.Next),
        )(">"),
        div(
          `class` := s"type-arrow",
          curPage == totalPage match
            case true =>
              style(Style("color" -> "lightgray"))
            case false =>
              onClick(PageMoveMsg.Patch(totalPage.toString())),
        )(">>"),
      ),
    )

  val search_tx_old = (model: Model) =>
    div(
      `class` := s"${State.curPage(model, PageName.DashBoard, "_search")} table-search xy-center ",
    )(
      div(`class` := "xy-center")(
        div(
          `class` := s"type-arrow ${_hidden[Int](1, model.tx_CurrentPage)}",
          onClick(PageMoveMsg.Patch("1")),
        )("<<"),
        div(
          `class` := s"type-arrow ${_hidden[Int](1, model.tx_CurrentPage)}",
          onClick(PageMoveMsg.Prev),
        )("<"),
        div(`class` := "type-plain-text")("Page"),
        input(
          onInput(s => PageMoveMsg.Get(s)),
          value   := s"${model.tx_list_Search}",
          `class` := "type-search xy-center DOM-page1 ",
        ),
        div(`class` := "type-plain-text")("of"),
        div(`class` := "type-plain-text")(model.tx_TotalPage.toString()),
        div(
          `class` := s"type-arrow ${_hidden[Int](model.tx_TotalPage, model.tx_CurrentPage)}",
          onClick(PageMoveMsg.Next),
        )(">"),
        div(
          `class` := s"type-arrow ${_hidden[Int](model.tx_TotalPage, model.tx_CurrentPage)}",
          onClick(PageMoveMsg.Patch(model.tx_TotalPage.toString())),
        )(">>"),
      ),
    )
