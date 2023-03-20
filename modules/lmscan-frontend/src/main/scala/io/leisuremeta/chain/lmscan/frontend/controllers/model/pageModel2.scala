package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.frontend.Log.log

// object Builder2:
//   def getNew(observers: List[ObserverState], find: Int) =
//     log(observers.filter(o => o.number == find))
//     observers.takeRight(1)(0)
//     observers.filter(o => o.number == find)(0)
//   def getPage(observers: List[ObserverState], find: Int) =
//     // 최신 상태에서 page 를 만듦
//     getNew(observers, find).pageCase
//   def getNumber(observers: List[ObserverState], find: Int) =
//     // 최신 상태에서 page 를 만듦
//     getNew(observers, find).number

//   def getData(observers: List[ObserverState], find: Int) =
//     // 최신 상태에서 page 를 만듦
//     getNew(observers, find).data

trait SubCase:
  def data: String

object SubCase:
  case class txSub(data: String)    extends SubCase
  case class blockSub(data: String) extends SubCase

  case class txDetailSub(data: String)      extends SubCase
  case class accountDetailSub(data: String) extends SubCase
  case class nftDetailSub(data: String)     extends SubCase
  case class blockDetailSub(data: String)   extends SubCase

trait PubCase:
  def page: Int

object PubCase:
  case class txPub(page: Int)            extends PubCase
  case class blockPub(page: Int)         extends PubCase
  case class txDetailPub(page: Int)      extends PubCase
  case class accountDetailPub(page: Int) extends PubCase
  case class nftDetailPub(page: Int)     extends PubCase
  case class blockDetailPub(page: Int)   extends PubCase

trait PageCase2:
  def name: String
  def url: String
  def subs: List[SubCase]
  def pubs: List[PubCase]

object PageCase2:

  case class DashBoard(
      name: String = "DashBoard",
      url: String = "DashBoard",
      pubs: List[PubCase] = List(PubCase.txPub(1), PubCase.blockPub(1)),
      subs: List[SubCase],
  ) extends PageCase

  case class Transactions(
      name: String = "Transactions",
      url: String = "Transactions",
      pubs: List[PubCase] = List(PubCase.txPub(1)),
      subs: List[SubCase],
  ) extends PageCase

  case class Blocks(
      name: String = "Blocks",
      url: String = "Blocks",
      pubs: List[PubCase] = List(PubCase.blockPub(1)),
      subs: List[SubCase],
  ) extends PageCase

  case class Observer(name: String = "Observer", url: String = "Observer")
      extends PageCase

  case class NoPage(name: String = "noPage", url: String = "noPage")
      extends PageCase
