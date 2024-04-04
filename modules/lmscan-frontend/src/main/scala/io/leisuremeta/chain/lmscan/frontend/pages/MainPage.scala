package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._

object MainPage:
  def update(model: BaseModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => 
      (model, Cmd.Batch(
        DataProcess.getData(BlcModel()),
        DataProcess.getData(TxModel()),
        DataProcess.getLocal("board"),
        DataProcess.getLocal("chart"),
        Nav.pushUrl(model.url),
      ))
    case RefreshData =>
      (model, Cmd.Batch(
        DataProcess.getData(BlcModel()),
        DataProcess.getData(TxModel()),
        DataProcess.getLocal("board"),
        DataProcess.getLocal("chart"),
      ))
    case UpdateModel(v: SummaryBoard) => (model.copy(summary = Some(v)), Cmd.None)
    case UpdateChart(v: SummaryChart) => (model.copy(chartData = Some(v)), Cmd.None)
    case UpdateBlcs(v) => (model.copy(blcs = Some(v)), Cmd.None)
    case UpdateTxs(v) => (model.copy(txs = Some(v)), Cmd.None)
    case GetDataFromApi(key) => (model, DataProcess.getDataAll(key))
    case SetLocal(key, d) => (model, DataProcess.setLocal(key, d))
    case msg: GlobalMsg => (model.copy(global = model.global.update(msg)), Cmd.None)
    case msg => (model.toEmptyModel, Cmd.emit(msg))

  def view(model: BaseModel): Html[Msg] =
    DefaultLayout.view(
      model,
      List( 
        BoardView.view(model),
        Table.mainView(model),
      )
    )

final case class BaseModel(
    global: GlobalModel = GlobalModel(),
    summary: Option[SummaryBoard] = None,
    chartData: Option[SummaryChart] = None,
    blcs: Option[PageResponse[BlockInfo]] = None,
    txs: Option[PageResponse[TxInfo]] = None,
) extends Model:
    def view: Html[Msg] = MainPage.view(this)
    def url = "/"
    def update: Msg => (Model, Cmd[IO, Msg]) = MainPage.update(this)
