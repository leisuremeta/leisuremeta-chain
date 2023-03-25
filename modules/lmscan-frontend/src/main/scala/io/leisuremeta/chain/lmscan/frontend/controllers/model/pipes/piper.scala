package io.leisuremeta.chain.lmscan.frontend
import scala.scalajs.js
import io.leisuremeta.chain.lmscan.frontend.Log.log
import io.leisuremeta.chain.lmscan.common.model.*
import io.leisuremeta.chain.lmscan.frontend.StateCasePipe.*
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.find_cunrrent_PageCase
import io.leisuremeta.chain.lmscan.frontend.PageCasePipe.in_PubCases

object Builder:

  // # PageCase |> [Pubcase] |> [pub_m2] |> ViewCase(tx,block,.....)
  def pipe_PageCase_ViewCase(pageCase: PageCase): ViewCase =

    // ViewCase 재할당이 일어나는 구조이므로, 리팩토링 필요할듯
    var resulte = new ViewCase()
    in_PubCases(pageCase)
      .map(d =>
        d match
          case PubCase.BlockPub(_, _, pub_m2) =>
            resulte.blockInfo = pub_m2.payload.toList

          case PubCase.TxPub(_, _, _, _, pub_m2) =>
            resulte.txInfo = pub_m2.payload.toList

          case _ =>
            "no",
      )
    resulte

  def pipe_PageCase_PageResponseViewCase(
      pageCase: PageCase,
  ): PageResponseViewCase =

    // ViewCase 재할당이 일어나는 구조이므로, 리팩토링 필요할듯
    var resulte = new PageResponseViewCase()
    in_PubCases(pageCase)
      .map(d =>
        d match
          case PubCase.BlockPub(_, _, pub_m2) =>
            resulte.block = pub_m2

          case PubCase.TxPub(_, _, _, _, pub_m2) =>
            resulte.tx = pub_m2

          case PubCase.BoardPub(_, _, pub_m2) =>
            resulte.board = pub_m2

          case PubCase.BlockDetailPub(_, _, pub_m2) =>
            resulte.blockDetail = pub_m2

          case PubCase.TxDetailPub(_, _, pub_m2) =>
            resulte.txDetail = pub_m2

          case PubCase.AccountDetailPub(_, _, pub_m2) =>
            resulte.accountDetail = pub_m2,
      )
    resulte

  // api 함수 정리필요
  def getViewCase(model: Model): ViewCase =
    pipe_PageCase_ViewCase(
      find_cunrrent_PageCase(model),
    )
  def getPubData(model: Model): PageResponseViewCase =
    pipe_PageCase_PageResponseViewCase(
      find_cunrrent_PageCase(model),
    )
  def update_PageCase_PubCases(pageCase: PageCase, pub: PubCase) =
    pageCase match
      // fixed :: create class => copy class 로 변경
      case pageCase: PageCase.Blocks =>
        pageCase.copy(pubs = in_PubCases(pageCase) ++ List(pub))
      case pageCase: PageCase.Transactions =>
        pageCase.copy(pubs = in_PubCases(pageCase) ++ List(pub))
      case pageCase: PageCase.DashBoard =>
        pageCase.copy(pubs = in_PubCases(pageCase) ++ List(pub))
      case pageCase: PageCase.BlockDetail =>
        pageCase.copy(pubs = in_PubCases(pageCase) ++ List(pub))
      case pageCase: PageCase.TxDetail =>
        pageCase.copy(pubs = in_PubCases(pageCase) ++ List(pub))
      case pageCase: PageCase.AccountDetail =>
        pageCase.copy(pubs = in_PubCases(pageCase) ++ List(pub))
      case pageCase: PageCase.Observer =>
        pageCase.copy(pubs = in_PubCases(pageCase) ++ List(pub))

  def pipe_pubcase_apiUrl(pub: PubCase) =
    var base = js.Dynamic.global.process.env.BASE_API_URL
    pub match

      case PubCase.BoardPub(page, _, _) =>
        s"$base/summary/main"

      case PubCase.BlockPub(page, _, _) =>
        s"$base/block/list?pageNo=${(page - 1).toString()}&sizePerRequest=10"

      case PubCase.TxPub(page, sizePerRequest, accountAddr, _, _) =>
        s"$base/tx/list?pageNo=${(page - 1)
            .toString()}&sizePerRequest=${sizePerRequest}" ++ {
          accountAddr match
            case "" => ""
            case _  => s"&accountAddr=${accountAddr}"
        }

      case PubCase.BlockDetailPub(hash, _, _) =>
        s"$base/block/$hash/detail"

      case PubCase.TxDetailPub(hash, _, _) =>
        s"$base/tx/$hash/detail"

      case PubCase.AccountDetailPub(hash, _, _) =>
        s"$base/account/$hash/detail"

  def update_PubCase_m1m2(pub: PubCase, data: String) =
    pub match
      case PubCase.BlockPub(_, _, _) =>
        PubCase.BlockPub(
          pub_m1 = data,
          pub_m2 = BlockParser
            .decodeParser(data)
            .getOrElse(new PageResponse(0, 0, List())),
        )

      case PubCase.TxPub(_, _, _, _, _) =>
        PubCase.TxPub(
          pub_m1 = data,
          pub_m2 = TxParser
            .decodeParser(data)
            .getOrElse(new PageResponse(0, 0, List())),
        )

      case PubCase.BoardPub(_, _, _) =>
        PubCase.BoardPub(
          pub_m1 = data,
          pub_m2 = ApiParser
            .decodeParser(data)
            .getOrElse(new SummaryModel),
        )

      // todo
      case PubCase.BlockDetailPub(_, _, _) =>
        PubCase.BlockDetailPub(
          pub_m1 = data,
          pub_m2 = BlockDetailParser
            .decodeParser(data)
            .getOrElse(new BlockDetail),
        )
      case PubCase.TxDetailPub(_, _, _) =>
        PubCase.TxDetailPub(
          pub_m1 = data,
          pub_m2 = TxDetailParser
            .decodeParser(data)
            .getOrElse(new TxDetail),
        )
      case PubCase.AccountDetailPub(_, _, _) =>
        PubCase.AccountDetailPub(
          pub_m1 = data,
          pub_m2 = AccountDetailParser
            .decodeParser(data)
            .getOrElse(new AccountDetail),
        )
