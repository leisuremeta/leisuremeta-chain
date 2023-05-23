package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.frontend.Log.log

object ValidPageName:
  def getPageFromString(search: String): PageCase | CommandCaseMode |
    CommandCaseLink =
    search match
      case ":on"    => CommandCaseMode.Development
      case ":off"   => CommandCaseMode.Production
      case ":prod"  => CommandCaseLink.Production
      case ":dev"   => CommandCaseLink.Development
      case ":local" => CommandCaseLink.Local
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
      case s"/blocks/${page}"
          if page.forall(
            Character.isDigit,
          ) =>
        PageCase.Blocks(
          url = s"blocks/${limit_value(page)}",
          pubs = List(PubCase.BlockPub(page = limit_value(page))),
        )
      case s"/transactions" => PageCase.Transactions()
      case s"/transactions/${page}"
          if page.forall(
            Character.isDigit,
          ) =>
        PageCase.Transactions(
          url = s"transactions/${limit_value(page)}",
          pubs = List(PubCase.TxPub(page = limit_value(page))),
        )
      case s"/txs" => PageCase.Transactions()
      case s"/txs/${page}"
          if page.forall(
            Character.isDigit,
          ) =>
        PageCase.Transactions(
          url = s"transactions/${limit_value(page)}",
          pubs = List(PubCase.TxPub(page = limit_value(page))),
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
      case s"/block/${hash}" if hash.length() == 64 =>
        PageCase.BlockDetail(
          name = PageCase.Blocks().name,
          url = s"block/${hash}",
          pubs = List(
            PubCase.BlockDetailPub(
              hash = hash,
            ),
            PubCase.TxPub(
              page = 1,
              blockHash = hash,
              sizePerRequest = 10,
            ),
          ),
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
