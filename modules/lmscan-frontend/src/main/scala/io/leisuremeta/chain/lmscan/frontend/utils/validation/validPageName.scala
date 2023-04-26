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
          case 42 =>
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
            PageCase.NftDetail(
              url = s"nft/${search}",
              pubs = List(PubCase.NftDetailPub(hash = search)),
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
      case s"/blocks/${page}" if page.toInt < 50000 =>
        PageCase.Blocks(
          url = s"blocks/${page}",
          pubs = List(PubCase.BlockPub(page = page.toInt)),
        )
      case s"/transactions" => PageCase.Transactions()
      case s"/transactions/${page}" if page.toInt < 50000 =>
        PageCase.Transactions(
          url = s"transactions/${page}",
          pubs = List(PubCase.TxPub(page = page.toInt)),
        )
      case s"/txs" => PageCase.Transactions()
      case s"/txs/${page}" if page.toInt < 50000 =>
        PageCase.Transactions(
          url = s"transactions/${page}",
          pubs = List(PubCase.TxPub(page = page.toInt)),
        )

      case s"/transaction/${hash}" if hash.length() == 64 =>
        PageCase.TxDetail(
          name = PageCase.Transactions().name,
          url = s"transaction/${hash}",
          pubs = List(PubCase.TxDetailPub(hash = hash)),
        )
      case s"/tx/${hash}" if hash.length() == 64 =>
        PageCase.TxDetail(
          name = PageCase.Transactions().name,
          url = s"transaction/${hash}",
          pubs = List(PubCase.TxDetailPub(hash = hash)),
        )

      case s"/nft/${hash}" if hash.length() == 25 =>
        PageCase.NftDetail(
          url = s"nft/${hash}",
          pubs = List(PubCase.NftDetailPub(hash = hash)),
        )
      case s"/account/${hash}" =>
        PageCase.AccountDetail(
          name = PageCase.AccountDetail().name,
          url = s"account/${hash}",
          pubs = List(
            PubCase.AccountDetailPub(hash = hash),
            PubCase.TxPub(
              page = 1,
              accountAddr = hash,
              sizePerRequest = 10,
            ),
          ),
        )

      case _ => PageCase.DashBoard()
