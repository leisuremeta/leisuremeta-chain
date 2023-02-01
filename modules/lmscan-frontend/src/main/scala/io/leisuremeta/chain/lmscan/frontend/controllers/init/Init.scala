package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO

object Init:
  val page              = NavMsg.DashBoard
  val toggle            = true
  val tx_CurrentPage    = 1
  val tx_TotalPage      = 1
  val block_CurrentPage = 1
  val block_TotalPage   = 1
  val block_list_Search = "1"
  val tx_list_Search    = "1"

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (
      Model(
        page,
        page,
        "",
        toggle,
        tx_CurrentPage,
        tx_TotalPage,
        block_CurrentPage,
        block_TotalPage,
        block_list_Search,
        tx_list_Search,
      ),
      OnTxMsg.getTxList(tx_CurrentPage.toString()),
      // Cmd.None,
    )
