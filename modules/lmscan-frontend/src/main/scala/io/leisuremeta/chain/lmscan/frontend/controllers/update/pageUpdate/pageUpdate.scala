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
          Window.History(page.name, page.name)

          (
            model.copy(
              observerNumber = getNumber(model.observers) + 1,
              observers = model.observers ++ Seq(
                ObserverState(
                  number = getNumber(model.observers) + 1,
                  pageCase = page,
                  data = "",
                ),
              ),
            ),
            Cmd.Batch(
              page.pubs.map(pub =>
                OnDataProcess.getData(
                  pub,
                ),
              ),
            ),
          )
    case PageMsg.GotoObserver(page: Int) =>
      val safeNumber = Num.Int_Positive(model.observerNumber - 1)
      Window.History(
        getPage(model.observers, safeNumber).name,
        getPage(model.observers, safeNumber).name,
      )

      (
        model.copy(observerNumber = page),
        Cmd.None,
      )
    case PageMsg.BackObserver =>
      val safeNumber = Num.Int_Positive(model.observerNumber - 1)

      Window.History(
        getPage(model.observers, safeNumber).name,
        getPage(model.observers, safeNumber).name,
      )

      (
        model.copy(observerNumber = safeNumber),
        Cmd.None,
      )

    case PageMsg.DataUpdate(sub: SubCase) =>
      (
        // 가장최신의 데이터 상태를 검색하여 업데이트
        model.copy(observers =
          model.observers.map(observer =>
            observer.number == model.observers.length match
              case true =>
                observer.copy(pageCase = observer.pageCase match
                  case PageCase.Blocks(_, _, _, _) =>
                    PageCase.Blocks(subs = observer.pageCase.subs ++ List(sub)),
                )
              case _ => observer,
          ),
        ),
        Cmd.None,
      )
