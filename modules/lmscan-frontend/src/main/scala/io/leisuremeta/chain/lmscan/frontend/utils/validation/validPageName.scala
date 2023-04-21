package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.frontend.Log.log

// enum PageName:
//   case DashBoard
//   case Blocks(page: Int)               extends PageName
//   case Transactions(page: Int)         extends PageName
//   case TransactionDetail(hash: String) extends PageName
//   case BlockDetail(hash: String)       extends PageName
//   case NftDetail(hash: String)         extends PageName
//   case AccountDetail(hash: String)     extends PageName
//   case Page64(hash: String)            extends PageName
//   case NoPage                          extends PageName

object ValidPageName:
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

  def getPageString(search: String): PageCase =
    search match
      // case "playnomm"    => PageName.AccountDetail(search.toString())
      // case "eth-gateway" => PageName.AccountDetail(search.toString())
      case _ =>
        search.length() match
          case 40 =>
            PageCase.AccountDetail(
              name = PageCase.AccountDetail().name,
              url = s"account/${search}",
              pubs = List(
                PubCase.AccountDetailPub(hash = search),
                PubCase.TxPub(
                  page = 1,
                  accountAddr = search,
                  sizePerRequest = 10,
                ),
              ),
            )
          case 25 =>
            PageCase.Blocks(
              url = s"blocks/${search}",
              pubs = List(PubCase.BlockPub(page = search.toInt)),
            )
          case 64 =>
            PageCase.TxDetail(
              name = PageCase.Transactions().name,
              url = s"transaction/${search}",
              pubs = List(PubCase.TxDetailPub(hash = search)),
            )
          case _ =>
            PageCase.AccountDetail(
              name = PageCase.AccountDetail().name,
              url = s"account/${search}",
              pubs = List(
                PubCase.AccountDetailPub(hash = search),
                PubCase.TxPub(
                  page = 1,
                  accountAddr = search,
                  sizePerRequest = 10,
                ),
              ),
            )

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

  def getPageCaseFromUrl(url: String): PageCase =
    url match
      case s"/dashboard" => PageCase.DashBoard()
      case s"/blocks"    => PageCase.Blocks()
      case s"/blocks/${page}" =>
        PageCase.Blocks(
          url = s"blocks/${page}",
          pubs = List(PubCase.BlockPub(page = page.toInt)),
        )
      case s"/transactions" => PageCase.Transactions()
      case s"/transactions/${page}" =>
        PageCase.Transactions(
          url = s"transactions/${page}",
          pubs = List(PubCase.TxPub(page = page.toInt)),
        )
      case s"/txs" => PageCase.Transactions()
      case s"/txs/${page}" =>
        PageCase.Transactions(
          url = s"transactions/${page}",
          pubs = List(PubCase.TxPub(page = page.toInt)),
        )

      case s"/transaction/${hash}" =>
        PageCase.TxDetail(
          name = PageCase.Transactions().name,
          url = s"transaction/${hash}",
          pubs = List(PubCase.TxDetailPub(hash = hash)),
        )
      case s"/tx/${hash}" =>
        PageCase.TxDetail(
          name = PageCase.Transactions().name,
          url = s"transaction/${hash}",
          pubs = List(PubCase.TxDetailPub(hash = hash)),
        )

      // case s"/dashboard"            => PageName.DashBoard
      // case s"/blocks"               => PageName.Blocks(1)
      // case s"/blocks/${page}"       => PageName.Blocks(page.toInt)
      // case s"/transactions"         => PageName.Transactions(1)
      // case s"/transactions/${page}" => PageName.Transactions(page.toInt)
      // case s"/block/${hash}"        => PageName.BlockDetail(hash)
      // case s"/tx/${hash}"           => PageName.TransactionDetail(hash)
      // case s"/transaction/${hash}"  => PageName.TransactionDetail(hash)
      // case s"/account/${hash}"      => PageName.AccountDetail(hash)
      // case s"/nft/${hash}"          => PageName.NftDetail(hash)

      case _ => PageCase.DashBoard()
