package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO

import tyrian.*
import tyrian.Html.*
import scala.scalajs.js.annotation.*
import org.scalajs.dom.KeyboardEvent
import org.scalajs.dom.window

@JSExportTopLevel("TyrianApp")
object LmscanFrontendApp extends TyrianApp[Msg, Model]:

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (Model(0, NavMsg.DashBoard, ""), Cmd.None)

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case navMsg: NavMsg     => NavUpdate.update(model)(navMsg)
    case inputMsg: InputMsg => InputUpdate.update(model)(inputMsg)
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
    Sub.Batch(
      Sub.fromEvent[IO, KeyboardEvent, Msg](
        "keydown",
        window.document.getElementsByClassName("sub-search").item(0),
      ) { e =>
        e.keyCode match
          case 13 =>
            // Enter key
            Some(InputMsg.Patch)
          case _ =>
            None
      },
    )

final case class Model(
    value: Int,
    tab: NavMsg,
    searchValue: String,
)

sealed trait Msg

enum NavMsg extends Msg:
  case DashBoard, Blocks, Transactions

enum InputMsg extends Msg:
  case Get(value: String) extends InputMsg
  case Patch              extends InputMsg

// enum NavMsg extends Msg:
//   case DashBoard(e: String)    extends NavMsg
//   case Blocks(e: String)       extends NavMsg
//   case Transactions(e: String) extends NavMsg
