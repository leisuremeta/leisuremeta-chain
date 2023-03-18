package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import Dom.{_hidden, timeAgo, yyyy_mm_dd_time, _selectedPage}

object Search:
  val search_block = (model: Model) =>
    div(
      `class` := s" table-search xy-center ",
    )(
      div(`class` := "xy-center")(
        div(
          `class` := s"type-arrow",
        )("<<"),
        div(
          `class` := s"type-arrow",
        )("<"),
        div(`class` := s"type-text-btn")(
        ),
        div(
          `class` := s"type-arrow",
        )(">"),
        div(
          `class` := s"type-arrow",
        )(">>"),
        div(
          style(Style("margin-left" -> "10px")),
        )(
          input(
            `class` := "type-search xy-center DOM-page1 margin-right text-center",
          ),
          div(`class` := "type-plain-text margin-right")("of"),
          div(`class` := "type-plain-text margin-right")("A"),
        ),
      ),
    )
