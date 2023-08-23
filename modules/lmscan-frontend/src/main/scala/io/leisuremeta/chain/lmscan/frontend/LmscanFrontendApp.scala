package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO

import tyrian.*
import tyrian.Html.*
import scala.scalajs.js.annotation.*

@JSExportTopLevel("TyrianApp")
object LmscanFrontendApp extends TyrianApp[Msg, Model]:
  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = (
    Model(
      commandMode = CommandCaseMode.Production,
      commandLink = CommandCaseLink.Production,
    ),
    Cmd.None,
  )

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    Update.update(model)

  def view(model: Model): Html[Msg] = 
    model.page match
      case "blocks" => BlockPage.view(model)
      case "transactions" => TxPage.view(model)
      case _ => MainPage.view(model)

  def subscriptions(model: Model): Sub[IO, Msg] =
    Subscriptions.subscriptions(model)

  def router: Location => Msg =
    case loc: Location.Internal =>
      loc.pathName match
        case "/dashboard" => RouterMsg.NavigateTo(MainPage)
        case "/blocks/1" => RouterMsg.NavigateTo(BlockPage)
        case "/transactions" => RouterMsg.NavigateTo(TxPage)
        case "/" => RouterMsg.NavigateTo(MainPage)
        case _   => RouterMsg.NoOp
    case loc: Location.External =>
      RouterMsg.NavigateToUrl(loc.href)
