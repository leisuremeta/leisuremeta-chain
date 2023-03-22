package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import io.leisuremeta.chain.lmscan.frontend.Builder.*

object Title:
  def block = (model: Model) =>
    div(
      // TODO :: only show dashboard
      `class` := s"table-title ${getPage(model) match
          case PageCase.Blocks(_, _, _, _) => "hidden"
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
          getPage(model) match
            case PageCase.DashBoard(_, _, _, _) =>
              span(
                onClick(PageMsg.PreUpdate(PageCase.Blocks())),
              )("More")
            case _ => div()
        },
      ),
    )
  def tx = (model: Model) =>
    div(
      `class` := s"table-title ${getPage(model) match
          case PageCase.Transactions(_, _, _, _) => "hidden"
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
          getPage(model) match
            case PageCase.DashBoard(_, _, _, _) =>
              span(
                onClick(PageMsg.PreUpdate(PageCase.Transactions())),
              )("More")
            case _ => div()
        },
      ),
    )

  // def nft = (model: Model) =>
  //   div(
  //     `class` := s"table-title ",
  //   )(
  //     div(
  //       `class` := s"type-1",
  //     )(span()("Item Activity")),
  //   )
