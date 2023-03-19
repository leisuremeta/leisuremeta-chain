package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import scala.scalajs.js
import org.scalajs.dom.window
import tyrian.*
import cats.effect.IO
import io.leisuremeta.chain.lmscan.frontend.Builder.*
// import io.leisuremeta.chain.lmscan.frontend.Model.observerNumber

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
            page.name,
            null,
            // show url
            page.name,
          )
          (
            model.copy(
              // curPage = page,
              observerNumber =
                getNumber(model.observers, model.observers.length) + 1,
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
      val safeNumber = (model.observerNumber - 1) < 1 match
        case true => 1
        case _    => model.observerNumber - 1

      window.history.pushState(
        // save page to history
        getPage(model.observers, safeNumber).name,
        null,
        // show url
        getPage(model.observers, safeNumber).name,
      )
      (
        model.copy(observerNumber = page),
        Cmd.None,
      )
    case PageMsg.BackObserver =>
      val safeNumber = (model.observerNumber - 1) < 1 match
        case true => 1
        case _    => model.observerNumber - 1

      window.history.pushState(
        // save page to history
        getPage(model.observers, safeNumber).name,
        null,
        // show url
        getPage(model.observers, safeNumber).name,
      )
      (
        model.copy(observerNumber = safeNumber),
        Cmd.None,
      )
    // case _ =>
    //   (
    //     model.copy(),
    //     Cmd.None,
    //   )
