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
  val Transactions = "24H TRANSACTIONS"
  val Accounts     = "TOTAL ACCOUNTS"

object BoardView:
  def view(model: Model): Html[Msg] =
    val data = get_PageResponseViewCase(model).board

    div(`class` := "board-area")(
      div(`class` := "board-list x ")(
        div(`class` := "loader-container")(
          div(`class` := "board-container xy-center  ")(
            {
              data != new SummaryModel match
                case false => div(`class` := "loader")()
                case _ =>
                  div(
                    `class` := "board-text y-center gap-10px",
                  )(
                    div(`class` := "font-16px color-white font-bold")(
                      Board.LM_Price,
                    ),
                    div(`class` := "color-white font-bold")(
                      plainStr(data.lmPrice).take(6) + " USDT",
                    ),
                  )
            },
          ),
        ),
        div(`class` := "loader-container")(
          div(`class` := "board-container xy-center  ")(
            {
              get_ViewCase(model).blockInfo(0) != new BlockInfo match
                case false => div(`class` := "loader")()
                case _ =>
                  div(
                    `class` := "board-text y-center gap-10px",
                  )(
                    div(`class` := "font-16px color-white font-bold")(
                      Board.Block_Number,
                    ),
                    div(`class` := "color-white font-bold")(
                      plainStr(
                        get_ViewCase(model).blockInfo(0).number,
                      ),
                    ),
                  )
            },
          ),
        ),
      ),
      div(`class` := "board-list x")(
        div(`class` := "loader-container")(
          div(`class` := "board-container xy-center  ")(
            {
              data != new SummaryModel match
                case false => div(`class` := "loader")()
                case _ =>
                  div(
                    `class` := "board-text y-center gap-10px",
                  )(
                    div(`class` := "font-16px color-white font-bold")(
                      Board.Transactions,
                    ),
                    div(`class` := "color-white font-bold")(
                      plainStr(data.txCountInLatest24h),
                    ),
                  )
            },
          ),
        ),
        div(`class` := "loader-container")(
          div(`class` := "board-container xy-center  ")(
            {
              data != new SummaryModel match
                case false => div(`class` := "loader")()
                case _ =>
                  div(
                    `class` := "board-text y-center gap-10px",
                  )(
                    div(`class` := "font-16px color-white font-bold")(
                      Board.Accounts,
                    ),
                    div(`class` := "color-white font-bold")(
                      plainStr(data.totalAccounts),
                    ),
                  )
            },
          ),
        ),
      ),
    )
