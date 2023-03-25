package io.leisuremeta.chain.lmscan.frontend
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.frontend.PupCasePipe.*

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

  def in_url(pageCase: PageCase) =
    pageCase match
      case PageCase.Blocks(_, url, _, _)        => url
      case PageCase.Transactions(_, url, _, _)  => url
      case PageCase.DashBoard(_, url, _, _)     => url
      case PageCase.BlockDetail(_, url, _, _)   => url
      case PageCase.TxDetail(_, url, _, _)      => url
      case PageCase.AccountDetail(_, url, _, _) => url
      case PageCase.Observer(_, url, _, _)      => url

  def in_PubCases(pageCase: PageCase): List[PubCase] =
    pageCase match
      case PageCase.Blocks(_, _, pubs, _)        => pubs
      case PageCase.Transactions(_, _, pubs, _)  => pubs
      case PageCase.DashBoard(_, _, pubs, _)     => pubs
      case PageCase.BlockDetail(_, _, pubs, _)   => pubs
      case PageCase.TxDetail(_, _, pubs, _)      => pubs
      case PageCase.AccountDetail(_, _, pubs, _) => pubs
      case PageCase.Observer(_, _, pubs, _)      => pubs

  // # PageCase |> [Pubcase] |> [in page] |> all page :: NOT USE
  def pipe_PageCase_PubCase__Page_All(pageCase: PageCase) =
    in_PubCases(pageCase)
      .map(d => in_Page(d))
      .reduce((a, b) => a + b)

  // # PageCase |> [Pubcase] |> [in pub_m1] |> all page :: NOT USE
  def pipe_PageCase_PubCase__pub_m1_All(pageCase: PageCase) =
    in_PubCases(pageCase)
      .map(d => in_pub_m1(d))
      .reduce((a, b) => a + b)
