package io.leisuremeta.chain.lmscan.frontend
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.frontend.PupCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.Log.log
import io.leisuremeta.chain.lmscan.common.model.TxInfo
import cats.instances.seq
import io.leisuremeta.chain.lmscan.frontend.Log.log2

object PageCasePipe:
  def in_Name(pageCase: PageCase) =
    pageCase match
      case PageCase.Blocks(name, _, _, _)        => name
      case PageCase.Transactions(name, _, _, _)  => name
      case PageCase.DashBoard(name, _, _, _)     => name
      case PageCase.BlockDetail(name, _, _, _)   => name
      case PageCase.TxDetail(name, _, _, _)      => name
      case PageCase.AccountDetail(name, _, _, _) => name
      case PageCase.Observer(name, _, _, _)      => name
      case PageCase.NftDetail(name, _, _, _)     => name
      case PageCase.NoPage(name, _, _, _)        => name

  def in_url(pageCase: PageCase) =
    pageCase match
      case PageCase.Blocks(_, url, _, _)        => url
      case PageCase.Transactions(_, url, _, _)  => url
      case PageCase.DashBoard(_, url, _, _)     => url
      case PageCase.BlockDetail(_, url, _, _)   => url
      case PageCase.TxDetail(_, url, _, _)      => url
      case PageCase.AccountDetail(_, url, _, _) => url
      case PageCase.Observer(_, url, _, _)      => url
      case PageCase.NftDetail(_, url, _, _)     => url
      case PageCase.NoPage(_, url, _, _)        => url

  def in_PubCases(pageCase: PageCase) =
    pageCase match
      case PageCase.Blocks(_, _, pubs, _)        => pubs
      case PageCase.Transactions(_, _, pubs, _)  => pubs
      case PageCase.DashBoard(_, _, pubs, _)     => pubs
      case PageCase.BlockDetail(_, _, pubs, _)   => pubs
      case PageCase.TxDetail(_, _, pubs, _)      => pubs
      case PageCase.AccountDetail(_, _, pubs, _) => pubs
      case PageCase.Observer(_, _, pubs, _)      => pubs
      case PageCase.NftDetail(_, _, pubs, _)     => pubs
      case PageCase.NoPage(_, _, pubs, _)        => pubs

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

  // # PageCase |> [Pubcase] |> [in page] |> all page :: NOT USE
  def pipe_PageCase_PubCase__Page_All(pageCase: PageCase) =
    pageCase
      .pipe(in_PubCases)
      .map(d => in_Page(d))
      .reduce((a, b) => a + b)

  // # PageCase |> [Pubcase] |> [in pub_m1] |> all page :: NOT USE
  def pipe_PageCase_PubCase__pub_m1_All(pageCase: PageCase) =
    pageCase
      .pipe(in_PubCases)
      .map(d => in_pub_m1(d))
      .reduce((a, b) => a + b)

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
            log2("PubCase.BoardPub")(pub_m2)
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
      case pageCase: PageCase.NftDetail =>
        pageCase.copy(pubs = in_PubCases(pageCase) ++ List(pub))
      case pageCase: PageCase.NoPage =>
        pageCase.copy(pubs = in_PubCases(pageCase) ++ List(pub))
