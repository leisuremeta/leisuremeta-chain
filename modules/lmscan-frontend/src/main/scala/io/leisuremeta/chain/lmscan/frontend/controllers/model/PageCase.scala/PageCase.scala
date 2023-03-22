package io.leisuremeta.chain.lmscan.frontend
import io.leisuremeta.chain.lmscan.common.model.*

enum PageCase:

  case Blocks(
      name: String = "Blocks",
      ulr: String = "Blocks",
      pubs: List[PubCase] = List(
        PubCase.blockPub(1, "", PageResponse[BlockInfo](0, 0, List())),
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
