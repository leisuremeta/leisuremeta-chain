package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

trait Page extends TyrianApp[Msg, Model]:

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = Update.update(model)

  def subscriptions(model: Model): Sub[IO, Msg] = Subscriptions.subscriptions(model)

  def router: Location => Msg = Routing.none(RouterMsg.NoOp)

object MainPage extends Page:
  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (
      Model(
        appStates = List(
          StateCase(
            DashBoard(),
            number = 1,
          ),
        ),
        commandMode = CommandCaseMode.Production,
        commandLink = CommandCaseLink.Production,
      ),
      Cmd.Batch(
        OnDataProcess.getData("a"),
        OnDataProcess.getData("b"),
        // OnDataProcess.getData("c"),
      )
    )

  def view(model: Model): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "pb-32px")(if (model.toggle) JsonPages.render(model) else Pages.render(model)),
    )
