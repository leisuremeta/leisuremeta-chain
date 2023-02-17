package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*

enum PageName:
  case DashBoard, Blocks, Transactions, NoPage
  case TransactionDetail(hash: String) extends PageName
  case BlockDetail(hash: String)       extends PageName
  case NftDetail(hash: String)         extends PageName
  case AccountDetail(hash: String)     extends PageName
  case Page64(hash: String)            extends PageName
  case None                            extends PageName

object ValidPageName:
  def getPage(search: PageName): PageName =
    search match
      case PageName.DashBoard            => search
      case PageName.Blocks               => search
      case PageName.Transactions         => search
      case PageName.BlockDetail(_)       => search
      case PageName.AccountDetail(_)     => search
      case PageName.TransactionDetail(_) => search
      case PageName.NftDetail(_)         => search
      case _ =>
        search.toString().length() match
          case 40 => PageName.AccountDetail(search.toString())
          case 25 => PageName.NftDetail(search.toString())
          case 64 => PageName.Page64(search.toString())
          case _  => PageName.None

  def getPageString(search: String): PageName =
    search.toString().length() match
      case 40 => PageName.AccountDetail(search.toString())
      case 25 => PageName.NftDetail(search.toString())
      case 64 => PageName.Page64(search.toString())
      case _  => PageName.None

  def getPageFromStr(search: String): PageName =
    search match
      case "DashBoard"                  => PageName.DashBoard
      case "Blocks"                     => PageName.Blocks
      case s"Transactions"              => PageName.Transactions
      case s"BlockDetail($hash)"        => PageName.BlockDetail(hash)
      case s"AccountDetail($hash)"      => PageName.AccountDetail(hash)
      case s"TransactionDetail($hash)"  => PageName.TransactionDetail(hash)
      case s"NftDetail($hash)"          => PageName.NftDetail(hash)
      case _                            => 
        search.length() match
          case 40 => PageName.AccountDetail(search)
          case 25 => PageName.NftDetail(search)
          case 64 => PageName.Page64(search)
          case _  => PageName.None      
