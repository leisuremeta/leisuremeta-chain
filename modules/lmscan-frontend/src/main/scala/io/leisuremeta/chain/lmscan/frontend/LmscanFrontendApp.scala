package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.*
import tyrian.Html.*
import scala.scalajs.js.annotation.*

@JSExportTopLevel("TyrianApp")
object LmscanFrontendApp extends TyrianApp[Msg, Model]:
  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = (Model(), Cmd.None)

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = Update.update(model)

  def view(model: Model): Html[Msg] = model.page.view(model)

  def subscriptions(model: Model): Sub[IO, Msg] = Subscriptions.subscriptions(model)

  def router: Location => Msg =
    case loc: Location.Internal =>
      loc.pathName match
        case "/dashboard" => RouterMsg.NavigateTo(MainPage)
        case s"/chart/$t" => t match
          case "account" => RouterMsg.NavigateTo(TotalAcChart)
          case "balance" => RouterMsg.NavigateTo(TotalBalChart)
          case _ => RouterMsg.NavigateTo(TotalTxChart)
        case s"/blocks/$page" => RouterMsg.NavigateTo(BlockPage(page.toInt))
        case s"/txs/$page" => RouterMsg.NavigateTo(TxPage(page.toInt))
        case s"/nfts/$page" => RouterMsg.NavigateTo(NftPage(page.toInt))
        case s"/accounts/$page" => RouterMsg.NavigateTo(AccountPage(page.toInt))
        case s"/tx/$hash" => RouterMsg.NavigateTo(TxDetailPage(hash))
        case s"/nft/$id/$page" => RouterMsg.NavigateTo(NftTokenPage(id, page.toInt))
        case s"/nft/$id" => RouterMsg.NavigateTo(NftDetailPage(id))
        case s"/block/$hash" => RouterMsg.NavigateTo(BlockDetailPage(hash))
        case s"/account/$hash" => RouterMsg.NavigateTo(AccountDetailPage(hash))
        case "/" => RouterMsg.NavigateTo(MainPage)
        case "" => RouterMsg.NavigateTo(MainPage)
        case _   => RouterMsg.NoOp
    case loc: Location.External =>
      RouterMsg.NavigateToUrl(loc.href)
