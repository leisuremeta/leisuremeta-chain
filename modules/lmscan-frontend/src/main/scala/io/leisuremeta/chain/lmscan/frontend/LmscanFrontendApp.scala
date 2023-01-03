package io.leisuremeta.chain.explorer.frontend

import cats.effect.IO

import tyrian.*
import tyrian.Html.*

object LmscanFrontendApp extends TyrianApp[Msg, Model]:

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (0, Cmd.None)

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case Msg.Increment => (model + 1, Cmd.None)
    case Msg.Decrement => (model - 1, Cmd.None)

  def view(model: Model): Html[Msg] =
    div()(
      button(onClick(Msg.Decrement))("-"),
      div()(model.toString),
      button(onClick(Msg.Increment))("+")
    )

  def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.None
    
type Model = Int

enum Msg:
  case Increment, Decrement
