package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO

import tyrian.*
import tyrian.Html.*
import scala.scalajs.js.annotation.*

@JSExportTopLevel("TyrianApp")
object LmscanFrontendApp extends TyrianApp[Msg, Model]:
  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (
      Model(
        appStates = List(
          StateCase(
            DashBoard(),
            number = 1,
          ),
        ),
        commandMode = CommandCaseMode.Production
            // case true => setMode(CommandCaseMode.Production)
            // case _ => setMode(CommandCaseMode.Development)
        ,
        commandLink = CommandCaseLink.Production
            // case true => CommandCaseLink.Production
            // case _    => CommandCaseLink.Development,
      ),
      Cmd.Batch(
        Cmd.Emit(PageMsg.PreUpdate(DashBoard())),
      ),
    )

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    Update.update(model)

  def view(model: Model): Html[Msg] =
    MainPage.view(model)

  def subscriptions(model: Model): Sub[IO, Msg] =
    Subscriptions.subscriptions(model)

  def router: Location => Msg = Routing.none(RouterMsg.NoOp)
