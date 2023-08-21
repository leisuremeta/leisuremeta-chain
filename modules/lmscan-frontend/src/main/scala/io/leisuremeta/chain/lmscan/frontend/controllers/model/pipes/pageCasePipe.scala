package io.leisuremeta.chain.lmscan.frontend
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.frontend.PupCasePipe.*
import io.leisuremeta.chain.lmscan.common.model.TxInfo
import cats.instances.seq

object PageCasePipe:
  def in_Name(pageCase: PageCase) = pageCase.name
  def in_url(pageCase: PageCase) = pageCase.url
  def in_PubCases(pageCase: PageCase) = pageCase.pubs

  // # PageCase |> [Pubcase] |> [pub_m2] |> ViewCase(tx,block,.....)
  // todo :: 리팩토링 필요
  def pipe_ViewCase(pageCase: PageCase): ViewCase =

    var resulte = new ViewCase()

    pageCase
      .pipe(in_PubCases)
      .map(pub =>
        pub match
          case PubCase.BlockPub(_, _, _, pub_m2) =>
            resulte.blockInfo = pub_m2.payload.toList

          case PubCase.TxPub(_, _, _, _, _, _, pub_m2) =>
            resulte.txInfo =
              val txInfoList = pub_m2.payload.toList
              txInfoList.length == 0 match
                case true  => List(new TxInfo)
                case false => txInfoList

          case _ =>
            "no",
      )
    resulte

  def pipe_PageResponseViewCase(pageCase: PageCase): PageResponseViewCase =
    var resulte = new PageResponseViewCase()
    pageCase
      .pipe(in_PubCases)
      .map(d =>
        d match
          case PubCase.BlockPub(_, _, _, pub_m2) =>
            resulte.block = pub_m2

          case PubCase.TxPub(_, _, _, _, _, _, pub_m2) =>
            resulte.tx = pub_m2

          case PubCase.BoardPub(_, _, pub_m2) =>
            resulte.board = pub_m2

          case PubCase.BlockDetailPub(_, _, pub_m2) =>
            resulte.blockDetail = pub_m2

          case PubCase.TxDetailPub(_, _, pub_m2) =>
            resulte.txDetail = pub_m2

          case PubCase.NftDetailPub(_, _, pub_m2) =>
            resulte.nftDetail = pub_m2

          case PubCase.AccountDetailPub(_, _, pub_m2) =>
            resulte.accountDetail = pub_m2,
      )
    resulte

  def update_PageCase_PubCases(pageCase: PageCase, pub: PubCase) =
    pageCase match
      // fixed :: create class => copy class 로 변경
      case pageCase: Blocks =>
        pageCase.copy(pubs = in_PubCases(pageCase) ++ List(pub))
      case pageCase: Transactions =>
        pageCase.copy(pubs = in_PubCases(pageCase) ++ List(pub))
      case pageCase: DashBoard =>
        pageCase.copy(pubs = in_PubCases(pageCase) ++ List(pub))
      case pageCase: BlockDetail =>
        pageCase.copy(pubs = in_PubCases(pageCase) ++ List(pub))
      case pageCase: TxDetail =>
        pageCase.copy(pubs = in_PubCases(pageCase) ++ List(pub))
      case pageCase: AccountDetail =>
        pageCase.copy(pubs = in_PubCases(pageCase) ++ List(pub))
      case pageCase: Observer =>
        pageCase.copy(pubs = in_PubCases(pageCase) ++ List(pub))
      case pageCase: NftDetail =>
        pageCase.copy(pubs = in_PubCases(pageCase) ++ List(pub))
      case pageCase: NoPage =>
        pageCase.copy(pubs = in_PubCases(pageCase) ++ List(pub))
