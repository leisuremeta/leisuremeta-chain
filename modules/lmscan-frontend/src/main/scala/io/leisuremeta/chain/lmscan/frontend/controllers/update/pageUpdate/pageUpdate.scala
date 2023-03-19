package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import scala.scalajs.js
import org.scalajs.dom.window
import tyrian.*
import cats.effect.IO
import io.leisuremeta.chain.lmscan.frontend.Builder.getNumber

case class ObserverState(pageCase: PageCase, number: Int)

object PageUpdate:
  def update(model: Model): PageMsg => (Model, Cmd[IO, Msg]) =
    case PageMsg.PreUpdate(page: PageCase) =>
      page match
        case PageCase.NoPage(_, _) =>
          (
            model.copy(),
            Cmd.None,
          )
        case _ =>
          window.history.pushState(
            // save page to history
            page.url,
            null,
            // show url
            page.url,
          )
          (
            model.copy(
              // curPage = page,
              observers = model.observers ++ Seq(
                ObserverState(
                  number =
                    getNumber(model.observers, model.observers.length) + 1,
                  pageCase = page,
                ),
              ),
            ),
            Cmd.None,
          )
    case PageMsg.UpdateObserver(page: Int) =>
      (
        model.copy(observerNumber = page),
        Cmd.None,
      )
    // case _ =>
    //   (
    //     model.copy(),
    //     Cmd.None,
    //   )
