package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO

object Init:
  val page                = PageName.DashBoard
  val toggle              = true
  val toggleTxDetailInput = true
  val tx_CurrentPage      = 1
  val tx_TotalPage        = 1
  val block_CurrentPage   = 1
  val block_TotalPage     = 1
  val block_list_Search   = "1"
  val tx_list_Search      = "1"

  // TODO :: could be list
  val apiCmd: Cmd.Batch[IO, Msg] =
    Cmd.Batch(OnDataProcess.getData(PageName.DashBoard))

  val txCmd: Cmd.Batch[IO, Msg] =
    Cmd.Batch(
      OnDataProcess.getData(
        PageName.Transactions,
        ApiPayload(page = tx_CurrentPage.toString()),
      ),
    )

  val blockCmd: Cmd.Batch[IO, Msg] =
    Cmd.Batch(
      OnDataProcess.getData(
        PageName.Blocks,
        ApiPayload(page = block_CurrentPage.toString()),
      ),
    )

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (
      Model(
        page,
        page,
        "",
        toggle,
        toggleTxDetailInput,
        tx_CurrentPage,
        tx_TotalPage,
        block_CurrentPage,
        block_TotalPage,
        block_list_Search,
        tx_list_Search,
      ),
      apiCmd ++ txCmd ++ blockCmd,
    )
