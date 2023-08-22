package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*

object Title:
  def box(str: String) =
    div(`class` := s"table-title hidden")(
      div(`class` := s"type-1")(span(str)),
      div(`class` := s"type-2")(div()),
    )
  def block = box("Latest Blocks")
  def tx = box("Latest transactions")
