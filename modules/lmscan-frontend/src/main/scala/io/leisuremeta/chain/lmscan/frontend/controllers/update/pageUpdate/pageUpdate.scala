package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import scala.scalajs.js
import org.scalajs.dom.window
import scala.util.chaining.*
import tyrian.*
import cats.effect.IO
import io.leisuremeta.chain.lmscan.frontend.Log.log
import io.leisuremeta.chain.lmscan.frontend.StateCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.PageCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*

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
            in_url(page),
            in_url(page),
          )
          (
            model.copy(
              pointer = get_latest_number(model) + 1,
              appStates = model.appStates ++ Seq(
                StateCase(
                  number = get_latest_number(model) + 1,
                  pageCase = page,
                ),
              ),
            ),
            Cmd.Batch(
              in_PubCases(page).map(pub =>
                OnDataProcess.getData(
                  pub,
                ),
              ),
            ),
          )

    case PageMsg.GotoObserver(page: Int) =>
      val safeNumber = Num.Int_Positive(page)
      Window.History(
        model
          .pipe(in_appStates)
          .pipe(find_PageCase(safeNumber))
          .pipe(in_url),
        model
          .pipe(in_appStates)
          .pipe(find_PageCase(safeNumber))
          .pipe(in_url),
      )

      (
        model.copy(pointer = page),
        Cmd.None,
      )

    case PageMsg.BackObserver =>
      val safeNumber = Num.Int_Positive(model.pointer - 1)

      Window.History(
        model
          .pipe(in_appStates)
          .pipe(find_PageCase(safeNumber))
          .pipe(in_url),
        model
          .pipe(in_appStates)
          .pipe(find_PageCase(safeNumber))
          .pipe(in_url),
      )

      (
        model.copy(pointer = safeNumber),
        Cmd.None,
      )

    case PageMsg.DataUpdate(pub: PubCase) =>
      log("pub")
      log(pub)
      (
        model.copy(
          appStates = model
            .pipe(in_appStates)
            .map(update_PubData(pub, model.appStates.length)),
        ),
        Cmd.None,
      )
