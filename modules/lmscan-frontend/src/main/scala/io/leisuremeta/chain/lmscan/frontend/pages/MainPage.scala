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
        DataProcess.getData(SummaryBoard()),
        DataProcess.getData(BlcModel()),
        DataProcess.getData(TxModel()),
        Nav.pushUrl(model.url),
      ))
    case UpdateModel(v: SummaryBoard) => (model.copy(summary = Some(v)), Cmd.None)
    case UpdateBlcs(v) => (model.copy(blcs = Some(v)), Cmd.None)
    case UpdateTxs(v) => (model.copy(txs = Some(v)), Cmd.None)
    case GlobalInput(s) => (model.copy(global = model.global.updateSearchValue(s)), Cmd.None)
    case msg => (model.toEmptyModel, Cmd.emit(msg))

  def view(model: BaseModel): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "color-white")(
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
