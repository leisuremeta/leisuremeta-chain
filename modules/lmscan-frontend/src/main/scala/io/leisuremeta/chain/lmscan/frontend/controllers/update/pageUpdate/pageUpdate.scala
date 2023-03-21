package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import scala.scalajs.js
import org.scalajs.dom.window
import tyrian.*
import cats.effect.IO
import io.leisuremeta.chain.lmscan.frontend.Builder.*
import io.leisuremeta.chain.lmscan.frontend.Log.log
// import io.leisuremeta.chain.lmscan.frontend.PageCase.Blocks.pubsub
import io.leisuremeta.chain.lmscan.common.model.PageResponse

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

    case PageMsg.DataUpdate(pubCase_m1: PubCase_M1) =>
      (
        // 가장최신의 데이터 상태를 검색하여 업데이트
        // pub 에 맞는 sub 을 찾게 해주는게 더 정확할것 같다
        model.copy(observers =
          model.observers.map(observer =>
            observer.number == model.observerNumber match
              case true =>
                observer.copy(pageCase = observer.pageCase match
                  case PageCase.Blocks(_, _, _, _, _) =>
                    PageCase.Blocks(
                      pubs_m1 = observer.pageCase.pubs_m1 ++ List(pubCase_m1),
                    ),
                )
              case _ => observer,
          ),
        ),
        Cmd.None,
      )

    //  .filter(d =>
    //                 d match
    //                   case SubCase.blockSub(_) => true
    //                   case _                   => false,
    //               )
