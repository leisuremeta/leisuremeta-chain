package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO

import tyrian.*
import tyrian.Html.*
import scala.scalajs.js.annotation.*

@JSExportTopLevel("TyrianApp")
object LmscanFrontendApp extends TyrianApp[Msg, Model]:
  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = (
    Model(),
    Cmd.None,
  )

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    Update.update(model)

  def view(model: Model): Html[Msg] = 
    model.page.view(model)
    //  match
    //   case "blocks" => BlockPage.view(model)
    //   case "transactions" => TxPage.view(model)
    //   case "tx" => TxDetailPage.view(model)
    //   case _ => MainPage.view(model)

  def subscriptions(model: Model): Sub[IO, Msg] =
    Subscriptions.subscriptions(model)

  def router: Location => Msg =
    case loc: Location.Internal =>
      loc.pathName match
        case "/dashboard" => RouterMsg.NavigateTo(MainPage)
        case s"/blocks/$page" => RouterMsg.NavigateTo(BlockPage)
        case s"/txs/$page" => RouterMsg.NavigateTo(TxPage)
        case s"/tx/${hash}" => RouterMsg.NavigateTo(TxDetailPage(hash))
        case s"/block/${hash}" => RouterMsg.NavigateTo(BlockDetailPage(hash))
        case "/" => RouterMsg.NavigateTo(MainPage)
        case _   => RouterMsg.NoOp
    case loc: Location.External =>
      RouterMsg.NavigateToUrl(loc.href)
