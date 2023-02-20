package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object State:
  def curPage = (model: Model, state: PageName | Msg, id: String) =>
    s"state ${state.toString()} ${id} ${model.curPage
        .toString() == state.toString()}"
  def toggle = (model: Model, state: PageName | Msg, id: String) =>
    s"state ${state.toString()} ${id} ${model.toggle}"

  def toggleTxDetailInput = (model: Model, state: Msg, id: String) =>
    s"state ${state.toString()} ${id} ${model.toggleTxDetailInput}"
