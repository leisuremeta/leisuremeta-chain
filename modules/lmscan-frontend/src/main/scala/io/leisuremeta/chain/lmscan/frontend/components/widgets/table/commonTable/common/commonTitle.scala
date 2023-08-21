package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*

object Title:
  def block = (model: Model) =>
    div(
      // TODO :: only show dashboard
      `class` := s"table-title ${find_current_PageCase(model) match
          case Blocks(_, _, _, _) => "hidden"
          case _                           => ""
        }",
    )(
      div(
        `class` := s"type-1",
      )(span()("Latest Blocks")),
      div(
        `class` := s"type-2",
      )(
        {
          find_current_PageCase(model) match
            case DashBoard(_, _, _, _) =>
              span(
                onClick(PageMsg.PreUpdate(Blocks())),
              )("More")
            case _ => div()
        },
      ),
    )
  def tx = (model: Model) =>
    div(
      `class` := s"table-title ${find_current_PageCase(model) match
          case Transactions(_, _, _, _) => "hidden"
          case _                                 => ""
        }",
    )(
      div(
        `class` := s"type-1",
      )(span()("Latest transactions")),
      div(
        `class` := s"type-2",
      )(
        {
          find_current_PageCase(model) match
            case DashBoard(_, _, _, _) =>
              span(
                onClick(PageMsg.PreUpdate(Transactions())),
              )("More")
            case _ => div()
        },
      ),
    )
