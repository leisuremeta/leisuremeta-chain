package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import scala.scalajs.js
import org.scalajs.dom.window
import tyrian.*
import cats.effect.IO
import io.leisuremeta.chain.lmscan.frontend.Builder.*
import io.leisuremeta.chain.lmscan.frontend.Log.log

object PageUpdate:
  def update(model: Model): PageMsg => (Model, Cmd[IO, Msg]) =
    // #flow1 ::
    // - Observers 에 새로운 ObserverState 를 추가한다
    // - observerNumber 를 최신으로 업데이트 한다
    // => #flow2
    case PageMsg.PreUpdate(page: PageCase) =>
      page match
        // case PageCase.NoPage(_, _) =>
        //   (
        //     model.copy(),
        //     Cmd.None,
        //   )
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
              observerNumber = getNumber(model.observers),
              observers = model.observers ++ Seq(
                ObserverState(
                  number = getNumber(model.observers) + 1,
                  pageCase = page,
                  data = "",
                ),
              ),
            ),

            // pubs
            Cmd.Batch(
              page.pubs.map(pub =>
                OnDataProcess.getData(
                  pub,
                ),
              ),
            ),
            // Cmd.None,
          )
    case PageMsg.GotoObserver(page: Int) =>
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

    case PageMsg.DataUpdate(sub: SubCase) =>
      // 가장 최신의 observer 상태 업데이트
      (
        model.copy(observers =
          model.observers.map(observer =>
            observer.number == model.observers.length match

              // 가장최신의 데이터인경우 => sub 를 pagecase의 subs에 넣어준다
              case true =>
                // observer
                val newSubs      = sub :: observer.pageCase.subs
                val newSubs_case = observer.pageCase
                observer.copy(pageCase = observer.pageCase match
                  case PageCase.Blocks(_, _, _, _) =>
                    PageCase.Blocks(subs = newSubs),
                )

              case _ => observer,
          ),
        ),
        Cmd.None,
      )
