package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*

object Title:
  def block = (model: Model) =>
    div(
      `class` := s"table-title hidden",
    )(
      div(
        `class` := s"type-1",
      )(span()("Latest Blocks")),
      div(
        `class` := s"type-2",
      )(
        div(),
      ),
    )
  def tx = (model: Model) =>
    div(
      `class` := s"table-title hidden",
    )(
      div(
        `class` := s"type-1",
      )(span()("Latest transactions")),
      div(
        `class` := s"type-2",
      )(
        div(),
      ),
    )
