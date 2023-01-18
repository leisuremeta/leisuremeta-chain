package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object BlockMainTable:
  def view = (model: Model) => CommonBlockTable.view(model)
