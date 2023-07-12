package io.leisuremeta.chain.lmscan.frontend
import io.leisuremeta.chain.lmscan.common.model.*

// enum PageCase(name: String = "", url: String = ""):
enum PageCase:
  case Observer(
      name: String = "Observer",
      url: String = "Observer",
      pubs: List[PubCase] = List(new PubCase.BoardPub),
      status: Boolean = false,
  ) extends PageCase

  case DashBoard(
      name: String = "Dashboard",
      url: String = "dashboard",
      pubs: List[PubCase] = List(
        new PubCase.BlockPub,
        new PubCase.TxPub,
        new PubCase.BoardPub,
      ),
      status: Boolean = false,
  ) extends PageCase

  case Blocks(
      name: String = "Blocks",
      url: String = "blocks/1",
      pubs: List[PubCase] = List(new PubCase.BlockPub),
      status: Boolean = false,
  ) extends PageCase

  case Transactions(
      name: String = "Transactions",
      url: String = "transactions/1",
      pubs: List[PubCase] = List(new PubCase.TxPub),
      status: Boolean = false,
  ) extends PageCase

  case BlockDetail(
      name: String = "Blocks",
      url: String = "block/hash...",
      pubs: List[PubCase] = List(
        PubCase.BlockDetailPub(),
      ),
      status: Boolean = false,
  ) extends PageCase

  case TxDetail(
      name: String = "Transactions",
      url: String = "transaction/hash...",
      pubs: List[PubCase] = List(
        PubCase.TxDetailPub(),
      ),
      status: Boolean = false,
  ) extends PageCase

  case NftDetail(
      name: String = "Transactions",
      url: String = "nft/hash...",
      pubs: List[PubCase] = List(
        PubCase.TxDetailPub(),
      ),
      status: Boolean = false,
  ) extends PageCase

  case AccountDetail(
      name: String = "Transactions",
      url: String = "account/hash...",
      pubs: List[PubCase] = List(
        PubCase.TxDetailPub(),
        PubCase.BoardPub(1, "", SummaryModel()),
      ),
      status: Boolean = false,
  ) extends PageCase

  case NoPage(
      name: String = "NoPage",
      url: String = "nopage",
      pubs: List[PubCase] = List(
      ),
      status: Boolean = false,
  ) extends PageCase

  // case DashBoard
  // case Transactions(page: Int)         extends PageCase
  // case TransactionDetail(hash: String) extends PageCase
  // case BlockDetail(hash: String)       extends PageCase
  // case NftDetail(hash: String)         extends PageCase
  // case AccountDetail(hash: String)     extends PageCase
  // case Page64(hash: String)            extends PageCase
  // case NoPage                          extends PageCase

// trait PageCase:
//   def name: String
//   def url: String
//   def pubs: List[PubCase]
//   def status: Boolean
//   // def pubsub: List[PageResponse[BlockInfo]] // todo :: fix

// object PageCase:
//   case class Blocks(
//       name: String = "Blocks",
//       url: String = "Blocks",
//       pubs: List[PubCase] = List(
//         PubCase.blockPub(1, "", PageResponse[BlockInfo](0, 0, List())),
//       ),
//       status: Boolean = false,
//   ) extends PageCase

// case class Transactions(
//     name: String = "Transactions",
//     url: String = "Transactions",
//     pubs: List[PubCase] = List(
//       PubCase.txPub(1, "", PageResponse[TxInfo](0, 0, List())),
//     ),
//     status: Boolean = false,
// ) extends PageCase
