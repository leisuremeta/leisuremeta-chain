package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object State:
  def curPage = (model: Model, state: Msg, id: String) =>
    s"state ${state.toString()} ${id} ${model.curPage
        .toString() == state.toString()}"
  def toggle = (model: Model, state: Msg, id: String) =>
    s"state ${state.toString()} ${id} ${model.toggle}"
