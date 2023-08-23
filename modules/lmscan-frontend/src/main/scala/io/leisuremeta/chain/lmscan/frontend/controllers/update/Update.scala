package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO

object Update:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case routerMsg: RouterMsg   => routerMsg match
      case RouterMsg.NavigateTo(page) => 
        // (model, Nav.pushUrl(page.name.toLowerCase))
        println(s"call update ${page.name}")
        page.update(model.copy(page = page))(routerMsg)
      case _ => (model, Cmd.None)
    case pageMsg: PageMsg       => PageUpdate.update(model)(pageMsg)
    case PopupMsg(v)     => (model.copy(popup = v), Cmd.None)
