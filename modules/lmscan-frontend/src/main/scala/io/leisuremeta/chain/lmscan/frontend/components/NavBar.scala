package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object NavBar:
  val main = "Dashboard"
  val blc = "Blocks"
  val tx = "Transactions"
  val acc = "Accounts"
  val nft = "NFTs"
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
        (acc, AccountPage(1)),
        (nft, NftPage(1)),
      ).map((name, page) =>
        div(
          `class` := "buttons",
        )(
          button(
            `class` := isActive(name, model),
            onClick(RouterMsg.NavigateTo(page)),
          )(span(name))
        )
      ),
    )

  def isActive(name: String, model: Model) = model match
    case m: BaseModel => if name == pageMatch(m.page) then "active" else ""
    case _: BlcDetailModel if name == blc => "active"
    case _: TxDetailModel if name == tx => "active"
    case _: AccDetailModel if name == acc => "active"
    case _: NftDetailModel if name == nft => "active"
    case _ => ""
  
  def pageMatch(page: Page): String =
    page match
      case MainPage => main
      case _: BlockPage => blc
      case _: TxPage => tx
      case _: NftPage => nft
      case _: AccountPage => acc
      case _ => ""

    
