package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO

object Update:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case routerMsg: RouterMsg   => routerMsg match
      case RouterMsg.NavigateTo(page) => 
        page.update(model.copy(page = page.name.toLowerCase))(routerMsg)
      case _ => (model, Cmd.None)
    case pageMsg: PageMsg       => PageUpdate.update(model)(pageMsg)
    case inputMsg: InputMsg     => SearchUpdate.update(model)(inputMsg)
    case toggleMsg: ToggleMsg   => ToggleUpdate.update(model)(toggleMsg)
    case popupMsg: PopupMsg     => PopupUpdate.update(model)(popupMsg)
    case commandMsg: CommandMsg => CommandUpdate.update(model)(commandMsg)
    case detailButtonMsg: DetailButtonMsg =>
      DetailButtonUpdate.update(model)(detailButtonMsg)
