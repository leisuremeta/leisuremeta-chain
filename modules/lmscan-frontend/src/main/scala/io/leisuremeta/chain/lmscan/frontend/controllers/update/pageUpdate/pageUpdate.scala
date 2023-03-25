package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import scala.scalajs.js
import org.scalajs.dom.window
import tyrian.*
import cats.effect.IO
import io.leisuremeta.chain.lmscan.frontend.Builder.*
import io.leisuremeta.chain.lmscan.frontend.Log.log
import io.leisuremeta.chain.lmscan.frontend.StateCasePipe.*

object PageUpdate:
  def update(model: Model): PageMsg => (Model, Cmd[IO, Msg]) =
    // #flow1 ::
    // - Observers 에 새로운 StateCase 를 추가한다
    // - pointer 를 최신으로 업데이트 한다
    // => #flow2
    case PageMsg.PreUpdate(page: PageCase) =>
      page match
        case _ =>
          Window.History(
            in_PageCase_url(page),
            in_PageCase_url(page),
          )
          (
            model.copy(
              pointer = in_Observer_Number(model.appStates) + 1,
              appStates = model.appStates ++ Seq(
                StateCase(
                  number = in_Observer_Number(model.appStates) + 1,
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
      val safeNumber = Num.Int_Positive(page)
      Window.History(
        in_PageCase_url(find_PageCase(safeNumber)(model.appStates)),
        in_PageCase_url(find_PageCase(safeNumber)(model.appStates)),
      )

      (
        model.copy(pointer = page),
        Cmd.None,
      )

    case PageMsg.BackObserver =>
      val safeNumber = Num.Int_Positive(model.pointer - 1)

      Window.History(
        in_PageCase_url(find_PageCase(safeNumber)(model.appStates)),
        in_PageCase_url(find_PageCase(safeNumber)(model.appStates)),
      )

      (
        model.copy(pointer = safeNumber),
        Cmd.None,
      )

    case PageMsg.DataUpdate(pub: PubCase) =>
      (
        // 가장최신의 데이터 상태를 검색하여 pubs 업데이트
        model.copy(appStates =
          model.appStates.map(observer =>
            observer.number == model.appStates.length match
              case true =>
                observer.copy(pageCase =
                  update_PageCase_PubCases(observer.pageCase, pub),
                )
              case _ => observer,
          ),
        ),
        Cmd.None,
      )
