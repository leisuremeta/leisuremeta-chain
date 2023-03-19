package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.frontend.Log.log

object Builder:
  def getNew(observers: List[ObserverState], find: Int) =
    log(observers.filter(o => o.number == find))
    observers.takeRight(1)(0)
    observers.filter(o => o.number == find)(0)
  def getPage(observers: List[ObserverState], find: Int) =
    // 최신 상태에서 page 를 만듦
    getNew(observers, find).pageCase
  def getNumber(observers: List[ObserverState], find: Int) =
    // 최신 상태에서 page 를 만듦
    getNew(observers, find).number

trait PageCase:
  def name: String
  def url: String

  // def page: Int

object PageCase:

  case class DashBoard(name: String = "DashBoard", url: String = "DashBoard")
      extends PageCase
  case class Observer(name: String = "Observer", url: String = "Observer")
      extends PageCase
  case class Blocks(name: String = "Blocks", url: String = "Blocks")
      extends PageCase
  case class Transactions(
      name: String = "Transactions",
      url: String = "Transactions",
  ) extends PageCase
  case class NoPage(name: String = "noPage", url: String = "noPage")
      extends PageCase

// case class PageCase ex:
//   def DashBoard(name: String = "DashBoard", url: String = "DashBoard")
//   def Observer(name: String = "Observer", url: String = "Observer")
//   def Blocks(page: Int, name: String = "Blocks", url: String = "Blocks")
//       extends PageName
//   def Transactions(
//       page: Int,
//       name: String = "Transactions",
//       url: String = "Transactions",
//   ) extends PageName
// case TransactionDetail(hash: String) extends PageName
// case BlockDetail(hash: String)       extends PageName
// case NftDetail(hash: String)         extends PageName
// case AccountDetail(hash: String)     extends PageName
// case Page64(hash: String)            extends PageName
// case NoPage                          extends PageName

// object ValidPageName:
//   def getPage(search: PageName): PageName =
//     search match
//       case PageName.DashBoard            => search
//       case PageName.Blocks(_)            => search
//       case PageName.Transactions(_)      => search
//       case PageName.BlockDetail(_)       => search
//       case PageName.AccountDetail(_)     => search
//       case PageName.TransactionDetail(_) => search
//       case PageName.NftDetail(_)         => search
//       case _ =>
//         search.toString().length() match
//           case 40 => PageName.AccountDetail(search.toString())
//           case 25 => PageName.NftDetail(search.toString())
//           case 64 => PageName.Page64(search.toString())
//           case _ =>
//             PageName.NoPage

//   def getPageString(search: String): PageName =
//     search.length() match
//       case 40 => PageName.AccountDetail(search.toString())
//       case 25 => PageName.NftDetail(search.toString())
//       case 64 => PageName.Page64(search.toString())
//       case _  => PageName.NoPage

//   def getPageFromStr(search: String): PageName =
//     search match
//       case "DashBoard"                 => PageName.DashBoard
//       case s"Blocks($page)"            => PageName.Blocks(page.toInt)
//       case s"Transactions($page)"      => PageName.Transactions(page.toInt)
//       case s"BlockDetail($hash)"       => PageName.BlockDetail(hash)
//       case s"AccountDetail($hash)"     => PageName.AccountDetail(hash)
//       case s"TransactionDetail($hash)" => PageName.TransactionDetail(hash)
//       case s"NftDetail($hash)"         => PageName.NftDetail(hash)
//       case _ =>
//         search.length() match
//           case 40 => PageName.AccountDetail(search)
//           case 25 => PageName.NftDetail(search)
//           case 64 => PageName.Page64(search)
//           case _  => PageName.NoPage

//   def getPageFromUrl(url: String): PageName =
//     url match
//       case s"/dashboard"            => PageName.DashBoard
//       case s"/blocks"               => PageName.Blocks(1)
//       case s"/blocks/${page}"       => PageName.Blocks(page.toInt)
//       case s"/transactions"         => PageName.Transactions(1)
//       case s"/transactions/${page}" => PageName.Transactions(page.toInt)
//       case s"/block/${hash}"        => PageName.BlockDetail(hash)
//       case s"/tx/${hash}"           => PageName.TransactionDetail(hash)
//       case s"/transaction/${hash}"  => PageName.TransactionDetail(hash)
//       case s"/account/${hash}"      => PageName.AccountDetail(hash)
//       case s"/nft/${hash}"          => PageName.NftDetail(hash)

//       case _ => PageName.NoPage
