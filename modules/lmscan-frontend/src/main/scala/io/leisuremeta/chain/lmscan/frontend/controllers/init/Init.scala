package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO

object Init:
  val page              = NavMsg.Transactions
  val toggle            = true
  val tx_CurrentPage    = 1
  val tx_TotalPage      = 100
  val block_CurrentPage = 1
  val block_TotalPage   = 100

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
      ),
      OnTxMsg.getTxList("random"),
    )
