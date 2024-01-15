package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import org.scalajs.dom._
import cats.effect.IO

object SearchView:
  def view(model: Model): Html[Msg] =
    div(`class` := "search-area")(
      div(`class` := "search-container xy-center")(
        input(
          id := "global-search",
          onInput(s => GlobalInput(s)),
          value   := s"${model.global.searchValue}",
          `class` := "search-text xy-center",
          `placeholder` := (
            "block hash, tx hash, account ... "
          ),
        ),
        div(
          onClick(GlobalSearch),
          `class` := "search-icon xy-center material-symbols-outlined",
        )(
          "search",
        ),
      ),
    )
  
  def detectSearch = 
    Sub.Batch(
      Option(document.getElementById("global-search")) match
        case None => Sub.None
        case Some(el) =>
          Sub.fromEvent[IO, KeyboardEvent, Msg]("keyup", el): e =>
            e.key match
              case "Enter" => Some(GlobalSearch)
              case _ => None
      ,
    )
