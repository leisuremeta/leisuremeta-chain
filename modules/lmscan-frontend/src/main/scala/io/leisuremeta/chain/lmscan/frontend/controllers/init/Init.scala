package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import scala.scalajs.js
import Log.*
import org.scalajs.dom.window
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
  val path =
    Log.log(window.location.pathname.toString().split("/").takeRight(2).toList)

  val path_match = log(
    path match
      case List("block", value) =>
        Cmd.Batch(
          Cmd.Emit(
            PageMsg.PreUpdate(
              PageName.BlockDetail(
                value,
              ),
            ),
          ),
        )
      case List("tx", value) =>
        Cmd.Batch(
          Cmd.Emit(
            PageMsg.PreUpdate(
              PageName.TransactionDetail(
                value,
              ),
            ),
          ),
        )
      case List("transaction", value) =>
        Cmd.Batch(
          Cmd.Emit(
            PageMsg.PreUpdate(
              PageName.TransactionDetail(
                value,
              ),
            ),
          ),
        )

      case List("account", value) =>
        Cmd.Batch(
          Cmd.Emit(
            PageMsg.PreUpdate(
              PageName.AccountDetail(
                value,
              ),
            ),
          ),
        )
      case List("nft", value) =>
        Cmd.Batch(
          Cmd.Emit(
            PageMsg.PreUpdate(
              PageName.NftDetail(
                value,
              ),
            ),
          ),
        )
      case _ =>
        window.history.pushState(
          null,
          null,
          s"${window.location.origin}",
        )
        apiCmd,
  )

  val setProtocol =
    if window.location.href
        .contains("http:") && !window.location.href.contains("local")
    then window.location.href = window.location.href.replace("http:", "https:")

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
      path_match ++ txCmd ++ blockCmd,
    )
