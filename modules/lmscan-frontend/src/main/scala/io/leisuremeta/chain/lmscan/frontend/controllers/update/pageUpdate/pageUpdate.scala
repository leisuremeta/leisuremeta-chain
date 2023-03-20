package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import scala.scalajs.js
import org.scalajs.dom.window
import tyrian.*
import cats.effect.IO
import io.leisuremeta.chain.lmscan.frontend.Builder.*

object PageUpdate:
  def update(model: Model): PageMsg => (Model, Cmd[IO, Msg]) =
    // #flow1 ::
    // - Observers 에 새로운 ObserverState 를 추가한다
    // - observerNumber 를 최신으로 업데이트 한다
    // => #flow2
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
                  data = "",
                ),
              ),
            ),
            Cmd.Batch(
              OnDataProcess.getData(
                page,
              ),
            ) ++ Cmd.Batch(
              OnDataProcess.getData(
                PageCase.Blocks(),
              ),
              OnDataProcess.getData(
                PageCase.Transactions(),
              ),
            ),
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

    case PageMsg.DataUpdate(data) =>
      // 가장 최신의 observer 상태 업데이트
      (
        model.copy(observers =
          model.observers.map(observer =>
            observer.number == model.observerNumber match
              case true => observer.copy(data = data)
              case _    => observer,
          ),
        ),
        Cmd.None,
      )
// kInfo(None,Some(6d2c247405be7730d79fbd9e7cb809285bf09e2b2db75ec3e6436b7fa459d7ca),None,Some(1679265162)))
// main.js:51 Uncaught org.scalajs.linker.runtime.UndefinedBehaviorError: java.lang.ClassCastException: number(0) cannot be cast to java.lang.Long
//     at $throwClassCastException (main.js:51:9)
//     at $uJ (main.js:518:95)
//     at $c_Lio_leisuremeta_chain_lmscan_frontend_V$.plainLong__s_Option__T (main.js:8687:16)
//     at main.js:9134:73
//     at $c_sjsr_AnonFunction1.apply__O__O (main.js:45223:41)
//     at $c_sci_ArraySeq.map__F1__sci_ArraySeq (main.js:98987:16)
//     at $c_sci_ArraySeq.map__F1__O (main.js:99080:15)
//     at $c_Lio_leisuremeta_chain_lmscan_frontend_gen$.cell__sci_Seq__sci_List (main.js:8810:39)
//     at f (main.js:6984:27)
//     at main.js:6993:39
