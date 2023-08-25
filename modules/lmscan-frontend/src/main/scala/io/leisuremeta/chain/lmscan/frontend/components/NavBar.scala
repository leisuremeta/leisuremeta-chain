package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object NavBar:
  val main = "Dashboard"
  val blc = "Blocks"
  val tx = "Transactions"
  def view(model: Model): Html[Msg] =
    nav()(
      div(id := "title", onClick(RouterMsg.NavigateTo(MainPage)))(
        span(id := "head")(img(id := "head-logo")),
      )
      ::
      List(
        (main, MainPage),
        (blc, BlockPage(1)),
        (tx, TxPage(1)),
      ).map((name, page) =>
        div(
          `class` := "buttons",
        )(
          button(
            `class` := s"${name == pageMatch(model.page)}",
            onClick(RouterMsg.NavigateTo(page)),
          )(span(name))
        )
      ),
    )
  
  def pageMatch(page: Page): String =
    page match
      case MainPage => main
      case _: BlockPage => blc
      case _: BlockDetailPage => blc
      case _: TxPage => tx
      case _: TxDetailPage => tx
      case _: NftDetailPage => tx
      case _: AccountDetailPage => tx
      case _ => ""

    
