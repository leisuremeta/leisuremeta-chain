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
          Window.History(in_PageCase_Name(page), in_PageCase_Name(page))
          (
            model.copy(
              observerNumber = in_Observer_Number(model.observers) + 1,
              observers = model.observers ++ Seq(
                ObserverState(
                  number = in_Observer_Number(model.observers) + 1,
                  pageCase = page,
                ),
              ),
            ),
            Cmd.Batch(
              in_PageCase_PubCases(page).map(pub =>
                OnDataProcess.getData(
                  pub,
                ),
              ),
            ),
          )
    case PageMsg.GotoObserver(page: Int) =>
      val safeNumber = Num.Int_Positive(model.observerNumber - 1)
      Window.History(
        in_PageCase_Name(in_Observer_PageCase(model.observers, safeNumber)),
        in_PageCase_Name(in_Observer_PageCase(model.observers, safeNumber)),
      )

      (
        model.copy(observerNumber = page),
        Cmd.None,
      )
    case PageMsg.BackObserver =>
      val safeNumber = Num.Int_Positive(model.observerNumber - 1)

      Window.History(
        in_PageCase_Name(in_Observer_PageCase(model.observers, safeNumber)),
        in_PageCase_Name(in_Observer_PageCase(model.observers, safeNumber)),
      )

      (
        model.copy(observerNumber = safeNumber),
        Cmd.None,
      )

    case PageMsg.DataUpdate(pub: PubCase) =>
      (
        // 가장최신의 데이터 상태를 검색하여 pubs 업데이트
        model.copy(observers =
          model.observers.map(observer =>
            observer.number == model.observerNumber match
              case true =>
                observer.copy(pageCase =
                  update_PageCase_PubCases(observer.pageCase, pub),
                )
              case _ => observer,
          ),
        ),
        Cmd.None,
      )
