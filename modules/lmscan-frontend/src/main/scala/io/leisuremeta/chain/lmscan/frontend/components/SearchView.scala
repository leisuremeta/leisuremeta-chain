package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import org.scalajs.dom._
import cats.effect.IO

object SearchView:
  def view(model: Model): Html[Msg] =
    div(cls := "search-area")(
      div(cls := "search-container")(
        input(
          id := "global-search",
          onInput(s => GlobalInput(s)),
          value   := s"${model.global.searchValue}",
          cls := "search-text",
          `placeholder` := (
            "Search by address, transaction, NFT, block",
          ),
        ),
        div(
          onClick(GlobalSearch),
          cls := "search-icon",
        )(
          "Search >>"
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
