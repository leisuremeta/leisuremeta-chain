package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object BlockDetailTable:
  def view = (model: Model) => CommonBlockTable.view(model)
