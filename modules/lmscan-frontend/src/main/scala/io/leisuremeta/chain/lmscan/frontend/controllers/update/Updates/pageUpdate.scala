package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import scala.scalajs.js
import org.scalajs.dom.window
import scala.util.chaining.*
import tyrian.*
import cats.effect.IO
import io.leisuremeta.chain.lmscan.frontend.StateCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.PageCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.PupCasePipe.in_Page
import io.leisuremeta.chain.lmscan.frontend.PupCasePipe.getPubCase
import io.leisuremeta.chain.lmscan.frontend.PupCasePipe.in_SummaryModel_pub
import io.leisuremeta.chain.lmscan.common.model.SummaryModel

object PageUpdate:
  def update(model: Model): PageMsg => (Model, Cmd[IO, Msg]) =
    case PageMsg.UpdateBlcs =>
      (model, Cmd.Batch(OnDataProcess.getData(model.blcPage)))
    case PageMsg.UpdateBlcsSearch(v: Int) =>
      (model.copy(blcPage = model.blcPage.copy(searchPage = v)), Cmd.None)
    case PageMsg.UpdateBlockPage(page: Int) =>
      (model.copy(blcPage = model.blcPage.copy(page = page)), Cmd.Emit(PageMsg.UpdateBlcs))
    case PageMsg.UpdateTxs =>
      (model, Cmd.Batch(OnDataProcess.getData(model.txPage)))
    case PageMsg.UpdateTxsSearch(v: Int) =>
      (model.copy(txPage = model.txPage.copy(searchPage = v)), Cmd.None)
    case PageMsg.UpdateTxPage(page: Int) =>
      (model.copy(txPage = model.txPage.copy(page = page)), Cmd.Emit(PageMsg.UpdateTxs))
    case PageMsg.UpdateTx(value: TxList) =>
      (model.copy(txPage = model.txPage.copy(list = value)), Cmd.None)
    case PageMsg.UpdateBlc(value: BlcList) =>
      (model.copy(blcPage = model.blcPage.copy(list = value)), Cmd.None)
    case PageMsg.Update1(value: SummaryModel) =>
      (model.copy(mainPage = model.mainPage.copy(summary = value)), Cmd.None)
    case PageMsg.Update2(value: BlcList) =>
      (model.copy(mainPage = model.mainPage.copy(bList = value)), Cmd.None)
    case PageMsg.Update3(value: TxList) =>
      (model.copy(mainPage = model.mainPage.copy(tList = value)), Cmd.None)

    case PageMsg.PreUpdate(page: PageCase) =>
      Window.History(
        in_url(page),
        in_url(page),
      )
      (
        model,
        Cmd.Batch(
          in_PubCases(page).map(pub =>
            OnDataProcess.getData(
              pub,
              model,
            ),
          ),
        ),
      )

    case PageMsg.GotoObserver(page: Int) =>
      val safeNumber = Num.Int_Positive(page)
      (
        model.copy(pointer = page),
        Cmd.None,
      )

    case PageMsg.BackObserver =>
      val safeNumber = Num.Int_Positive(model.pointer - 1)
      (
        model.copy(pointer = safeNumber),
        Cmd.None,
      )
    case PageMsg.RolloBack =>
      val safeNumber = Num.Int_Positive(model.pointer - 1)
      (
        model.copy(
          pointer = safeNumber,
        ),
        Cmd.None
      )

    case PageMsg.None => (model, Cmd.None)
