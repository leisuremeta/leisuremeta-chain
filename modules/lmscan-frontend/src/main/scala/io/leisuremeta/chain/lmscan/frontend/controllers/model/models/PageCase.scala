package io.leisuremeta.chain.lmscan.frontend
import io.leisuremeta.chain.lmscan.common.model.*

trait PageCase:
  val name: String
  val url: String
  val pubs: List[PubCase]
  val status: Boolean

final case class Observer(
    name: String = "Observer",
    url: String = "Observer",
    pubs: List[PubCase] = List(new PubCase.BoardPub),
    status: Boolean = false,
) extends PageCase

final case class DashBoard(
    name: String = "Dashboard",
    url: String = "dashboard",
    pubs: List[PubCase] = List(
      new PubCase.BlockPub,
      new PubCase.TxPub,
      new PubCase.BoardPub,
    ),
    status: Boolean = false,
) extends PageCase

final case class Blocks(
    name: String = "Blocks",
    url: String = "blocks/1",
    pubs: List[PubCase] = List(new PubCase.BlockPub),
    status: Boolean = false,
) extends PageCase

final case class Transactions(
    name: String = "Transactions",
    url: String = "transactions/1",
    pubs: List[PubCase] = List(new PubCase.TxPub),
    status: Boolean = false,
) extends PageCase

final case class BlockDetail(
    name: String = "Blocks",
    url: String = "block/hash...",
    pubs: List[PubCase] = List(
      PubCase.BlockDetailPub(),
    ),
    status: Boolean = false,
) extends PageCase

final case class TxDetail(
    name: String = "Transactions",
    url: String = "transaction/hash...",
    pubs: List[PubCase] = List(
      PubCase.TxDetailPub(),
    ),
    status: Boolean = false,
) extends PageCase

final case class NftDetail(
    name: String = "Transactions",
    url: String = "nft/hash...",
    pubs: List[PubCase] = List(
      PubCase.TxDetailPub(),
    ),
    status: Boolean = false,
) extends PageCase

final case class AccountDetail(
    name: String = "Transactions",
    url: String = "account/hash...",
    pubs: List[PubCase] = List(
      PubCase.TxDetailPub(),
      PubCase.BoardPub(1, "", SummaryModel()),
    ),
    status: Boolean = false,
) extends PageCase

final case class NoPage(
    name: String = "NoPage",
    url: String = "nopage",
    pubs: List[PubCase] = List(
    ),
    status: Boolean = false,
) extends PageCase
