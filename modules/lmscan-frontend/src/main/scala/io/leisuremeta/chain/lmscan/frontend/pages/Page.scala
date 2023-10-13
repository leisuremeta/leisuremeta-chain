package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

trait Page:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg])
  def view(model: Model): Html[Msg]
  def url: String

// trait ListPageX extends Page:
//   def updatePage(page: Int): Msg => (Model, Cmd[IO, Msg])
