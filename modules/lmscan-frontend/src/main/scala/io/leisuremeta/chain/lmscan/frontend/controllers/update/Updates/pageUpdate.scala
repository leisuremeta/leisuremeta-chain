package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import scala.scalajs.js
import org.scalajs.dom.window
import scala.util.chaining.*
import tyrian.*
import cats.effect.IO
import io.leisuremeta.chain.lmscan.common.model._

object PageUpdate:
  def update(model: Model): PageMsg => (Model, Cmd[IO, Msg]) =
    case PageMsg.UpdateBlcs =>
      (
        model,
        Cmd.Batch(
          OnDataProcess.getData(model.blcPage),
          Nav.pushUrl(s"/blocks/${model.blcPage.page}")
        )
      )
    case PageMsg.UpdateBlcsSearch(v: Int) =>
      (model.copy(blcPage = model.blcPage.copy(searchPage = v)), Nav.pushUrl(s"/blocks/$v"))
    case PageMsg.UpdateBlockPage(page: Int) =>
      (model.copy(blcPage = model.blcPage.copy(page = page)), Cmd.Emit(PageMsg.UpdateBlcs))
    case PageMsg.UpdateTxs =>
      (model, Cmd.Batch(
        OnDataProcess.getData(model.txPage),
        Nav.pushUrl(s"/txs/${model.txPage.page}"),
      ))
    case PageMsg.UpdateTxsSearch(v: Int) =>
      (
        model.copy(txPage = model.txPage.copy(searchPage = v)), 
        Nav.pushUrl(s"/txs/$v")
      )
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
      (model.copy(mainPage = model.mainPage.copy(tList = value)), Nav.pushUrl("/dashboard"))
    case PageMsg.UpdateTxDetailPage(hash: String) =>
      (model, Cmd.Batch(OnDataProcess.getData(TxDetail(hash = Some(hash)))))
    case PageMsg.UpdateTxDetail(v: TxDetail) =>
      (model.copy(txDetail = v), Nav.pushUrl(s"/tx/${v.hash.getOrElse("")}"))
    case PageMsg.UpdateBlcDetailPage(hash: String) =>
      (model, Cmd.Batch(OnDataProcess.getData(BlockDetail(hash = Some(hash)))))
    case PageMsg.UpdateBlcDetail(v: BlockDetail) =>
      (model.copy(blcDetail = v), Nav.pushUrl(s"/block/${v.hash.getOrElse("")}"))

    case PageMsg.GotoObserver(page: Int) =>
      val safeNumber = 0
      (
        model.copy(pointer = page),
        Cmd.None,
      )

    case PageMsg.BackObserver =>
      val safeNumber = 0
      (
        model.copy(pointer = safeNumber),
        Cmd.None,
      )
    case PageMsg.RolloBack =>
      val safeNumber = 0
      (
        model.copy(
          pointer = safeNumber,
        ),
        Cmd.None
      )

    case PageMsg.None => (model, Cmd.None)
