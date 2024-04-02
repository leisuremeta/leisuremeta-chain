package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

object DefaultLayout:
  def view(model: Model, contents: List[Html[Msg]]): Html[Msg] =
    div(cls := "main")(
      header(
        NavBar.view(model),
        SearchView.view(model),
      ),
      section(cls := "con-wrap")(contents),
      footer(Footer.view()),
    )
  def view(model: Model, contents: Html[Msg]): Html[Msg] =
    this.view(model, List(contents))
