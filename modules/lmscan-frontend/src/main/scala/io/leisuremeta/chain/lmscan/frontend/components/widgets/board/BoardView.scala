package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import V.*
import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
import io.leisuremeta.chain.lmscan.frontend.Log.log
import Dom.*
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import io.leisuremeta.chain.lmscan.common.model.BlockInfo

object Board:
  val LM_Price     = "LM PRICE"
  val Block_Number = "BLOCK NUMBER"
  val Transactions = "TOTAL DATA SIZE"
  val Accounts     = "TOTAL ACCOUNTS"

object BoardView:
  def view(model: Model): Html[Msg] =
    val data = get_PageResponseViewCase(model).board

    div(`class` := "board-area")(
      div(`class` := "board-list x ")(
        div(`class` := "board-container xy-center position-relative  ")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-white font-bold")(
              Board.LM_Price,
            ),
            div(`class` := "color-white font-bold")(
              plainStr(data.lmPrice).take(6) + " USDT",
            ),
          ), {
            data != new SummaryModel match
              case false =>
                LoaderView.view(model)

              case _ => div()
          },
        ),
        div(`class` := "board-container xy-center position-relative  ")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-white font-bold")(
              Board.Block_Number,
            ),
            div(`class` := "color-white font-bold")(
              plainStr(
                current_ViewCase(model).blockInfo(0).number,
              ),
            ),
          ), {
            current_ViewCase(model).blockInfo(0) != new BlockInfo match
              case false =>
                LoaderView.view(model)
              case _ => div()
          },
        ),
      ),
      div(`class` := "board-list x")(
        div(`class` := "board-container xy-center position-relative  ")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-white font-bold")(
              Board.Transactions,
            ),
            div(`class` := "color-white font-bold")(
              {
                // log("data.totalTxSize")
                // log(data.totalTxSize)
                String
                  .format(
                    "%.3f",
                    plainLong(
                      data.totalTxSize,
                    ).toDouble / (1024 * 1024).toDouble,
                  ) + " MB"
              },
            ),
          ), {
            data != new SummaryModel match
              case false =>
                LoaderView.view(model)
              case _ => div()
          },
        ),
        div(`class` := "board-container xy-center position-relative  ")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-white font-bold")(
              Board.Accounts,
            ),
            div(`class` := "color-white font-bold")(
              plainStr(data.totalAccounts),
            ),
          ), {
            data != new SummaryModel match
              case false =>
                LoaderView.view(model)
              case _ => div()
          },
        ),
      ),
    )
