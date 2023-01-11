package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO

import tyrian.*
import tyrian.Html.*
import scala.scalajs.js.annotation.*

@JSExportTopLevel("TyrianApp")
object LmscanFrontendApp extends TyrianApp[Msg, Model]:

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (Model(0, NavMsg.DashBoard), Cmd.None)

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case navMsg: NavMsg => NavUpdate.update(model)(navMsg)
    // case mainMsg: MainMsg =>
    //   mainMsg match
    //     case MainMsg.Increment =>
    //       (model.copy(value = model.value + 1), Cmd.None)
    //     case MainMsg.Decrement =>
    //       (model.copy(value = model.value - 1), Cmd.None)

  def view(model: Model): Html[Msg] =
    div(
      div(`class` := "main")(NavView.view(model), PagesView.view(model)),
    )

  def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.None

final case class Model(
    value: Int,
    tab: NavMsg,
)

sealed trait Msg

enum NavMsg extends Msg:
  case DashBoard, Blocks, Transactions
