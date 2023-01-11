package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object navButton:
  val s1 = "DashBoard"
  val s2 = "Blocks"
  val s3 = "Transactions"

// object isClick:
// def apply(state: Model) = state match
//   case

object NavView:
  def view(model: Model): Html[Msg] =
    nav(`class` := "bg-gray-800 h-screen w-[50px]")(
      div(id := "playnomm")("playNomm"),
      div(
        id := "buttons",
      )(
        button(
          `class` := s"${model.tab.toString() == "S1"}",
          onClick(NavMsg.S1),
        )(navButton.s1),
        button(
          `class` := s"${model.tab.toString() == "S2"}",
          onClick(NavMsg.S2),
        )(navButton.s2),
        button(
          `class` := s"${model.tab.toString() == "S3"}",
          onClick(NavMsg.S3),
        )(navButton.s3),
      ),
    )
