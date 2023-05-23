package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import scala.scalajs.js
import org.scalajs.dom.window
import scala.util.chaining.*
import tyrian.*
import cats.effect.IO
import io.leisuremeta.chain.lmscan.frontend.StateCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.PageCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
import io.leisuremeta.chain.lmscan.frontend.PupCasePipe.in_Page
import io.leisuremeta.chain.lmscan.frontend.Log.*
import io.leisuremeta.chain.lmscan.frontend.PupCasePipe.getPubCase

object PageUpdate:
  def update(model: Model): PageMsg => (Model, Cmd[IO, Msg]) =
    case PageMsg.PreUpdate(page: PageCase) =>
      page match
        case _ =>
          Window.History(
            in_url(page),
            in_url(page),
          )
          (
            model.copy(
              tx_current_page = page
                .pipe(in_PubCases)
                .pipe(getPubCase[PubCase.TxPub])
                .pipe(_.getOrElse(PubCase.TxPub()))
                .pipe(in_Page)
                .pipe(_.toString),
              block_current_page = page
                .pipe(in_PubCases)
                .pipe(getPubCase[PubCase.BlockPub])
                .pipe(_.getOrElse(PubCase.BlockPub()))
                .pipe(in_Page)
                .pipe(_.toString),
              tx_total_page =
                get_PageResponseViewCase(model).tx.totalPages match
                  case 1 => model.tx_total_page
                  case _ =>
                    get_PageResponseViewCase(model).tx.totalPages.toString()
              ,
              block_total_page =
                get_PageResponseViewCase(model).block.totalPages match
                  case 1 => model.block_total_page
                  case _ =>
                    get_PageResponseViewCase(model).block.totalPages.toString()
              ,
              pointer = get_latest_number(model) + 1,
              appStates = model.appStates ++ Seq(
                StateCase(
                  number = get_latest_number(model) + 1,
                  pageCase = page,
                ),
              ),
            ),
            Cmd.Batch(
              in_PubCases(page).map(pub =>
                OnDataProcess.getData(
                  pub,
                  model,
                ),
              ),
            ),
          )

    case PageMsg.GotoObserver(page: Int) =>
      val safeNumber = Num.Int_Positive(page)
      Window.History(
        model
          .pipe(in_appStates)
          .pipe(find_PageCase(safeNumber))
          .pipe(in_url),
        model
          .pipe(in_appStates)
          .pipe(find_PageCase(safeNumber))
          .pipe(in_url),
      )

      (
        model.copy(pointer = page),
        Cmd.None,
      )

    case PageMsg.BackObserver =>
      val safeNumber = Num.Int_Positive(model.pointer - 1)

      Window.History(
        model
          .pipe(in_appStates)
          .pipe(find_PageCase(safeNumber))
          .pipe(in_url),
        model
          .pipe(in_appStates)
          .pipe(find_PageCase(safeNumber))
          .pipe(in_url),
      )

      (
        model.copy(pointer = safeNumber),
        Cmd.None,
      )
    case PageMsg.RolloBack =>
      val safeNumber = Num.Int_Positive(model.pointer - 1)

      Window.History(
        model
          .pipe(in_appStates)
          .pipe(find_PageCase(safeNumber))
          .pipe(in_url),
        model
          .pipe(in_appStates)
          .pipe(find_PageCase(safeNumber))
          .pipe(in_url),
      )

      (
        model.copy(
          pointer = safeNumber,
          appStates = model.appStates.dropRight(1),
        ),
        Cmd.emit(
          PageMsg.PreUpdate(PageCase.NoPage(name = find_name(model))),
        ),
      )

    case PageMsg.DataUpdate(pub: PubCase) =>
      (
        model.copy(
          appStates = model
            .pipe(in_appStates)
            .map(update_PubData(pub, model.appStates.length)),
        ),
        Cmd.None,
      )
    case PageMsg.None => (model, Cmd.None)

    case PageMsg.GetFromBlockSearch(s) =>
      (model.copy(block_current_page = s), Cmd.None)

    case PageMsg.GetFromTxSearch(s) =>
      (model.copy(tx_current_page = s), Cmd.None)

    case PageMsg.PatchFromBlockSearch(validPage) =>
      (
        model.copy(block_current_page = validPage),
        Cmd.emit(
          PageMsg.PreUpdate(
            PageCase.Blocks(
              url = s"blocks/${validPage}",
              pubs = List(
                PubCase.BlockPub(page = validPage.toInt),
              ),
            ),
          ),
        ),
      )

    case PageMsg.PatchFromTxSearch(validPage) =>
      (
        model.copy(tx_current_page = validPage),
        Cmd.emit(
          PageMsg.PreUpdate(
            PageCase.Transactions(
              url = s"transactions/${validPage}",
              pubs = List(
                PubCase.TxPub(page = validPage.toInt),
              ),
            ),
          ),
        ),
      )
