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
import io.leisuremeta.chain.lmscan.frontend.PupCasePipe.getPubCase
import io.leisuremeta.chain.lmscan.frontend.PupCasePipe.in_SummaryModel_pub
import io.leisuremeta.chain.lmscan.common.model.SummaryModel

object PageUpdate:
  def update(model: Model): PageMsg => (Model, Cmd[IO, Msg]) =
    case PageMsg.Update1(value: SummaryModel) =>
      (model.copy(mainPage = model.mainPage.copy(summary = value)), Cmd.None)
    case PageMsg.Update2(value: BlcList) =>
      (model.copy(mainPage = model.mainPage.copy(blcList = value)), Cmd.None)
    case PageMsg.Update3(value: TxList) =>
      (model.copy(mainPage = model.mainPage.copy(txList = value)), Cmd.None)

    case PageMsg.PreUpdate(page: PageCase) =>
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
          PageMsg.PreUpdate(NoPage(name = find_name(model))),
        ),
      )

    case PageMsg.DataUpdate(pub: PubCase) =>
      (
        model.copy(
          // DataUpdate => 후처리 로직으로 바꾸는것 고려하기
          subtype = pub match
            case pub: PubCase.TxPub => pub.subtype
            case _                  => model.subtype
          ,
          tx_total_page = pub match
            case pub: PubCase.TxPub => pub.pub_m2.totalPages.toString()
            case _                  => model.tx_total_page
          ,
          block_total_page = pub match
            case pub: PubCase.BlockPub => pub.pub_m2.totalPages.toString()
            case _                     => model.block_total_page
          ,
          appStates = model
            .pipe(in_appStates)
            .map(update_PubData(pub, model.appStates.length)),
          lmprice = List(pub)
            .pipe(getPubCase[PubCase.BoardPub])
            .pipe(
              _.getOrElse(
                PubCase.BoardPub(pub_m2 =
                  SummaryModel(lmPrice = Some(model.lmprice)),
                ),
              ),
            )
            .pipe(in_SummaryModel_pub(model))
            .pipe(d => Math.floor(d * 10000) / 10000),
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
            Blocks(
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
            Transactions(
              url = s"transactions/${validPage}",
              pubs = List(
                PubCase.TxPub(page = validPage.toInt),
              ),
            ),
          ),
        ),
      )
