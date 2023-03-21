package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.frontend.Log.log

object Builder:
  def getObserver(observers: List[ObserverState], find: Int) =
    // find 가 0이면 가장 최신 옵져버로 검색할수 있게 해준다
    val _find = find match
      case 0 => observers.length
      case _ => find
    observers.filter(o => o.number == _find)(0)

  def getPage(observers: List[ObserverState], find: Int = 0) =
    getObserver(observers, find).pageCase

  def getNumber(observers: List[ObserverState], find: Int = 0) =
    getObserver(observers, find).number

trait PubCase:
  def page: Int

object PubCase:
  case class txPub(page: Int)            extends PubCase
  case class blockPub(page: Int)         extends PubCase
  case class txDetailPub(page: Int)      extends PubCase
  case class accountDetailPub(page: Int) extends PubCase
  case class nftDetailPub(page: Int)     extends PubCase
  case class blockDetailPub(page: Int)   extends PubCase
  case class NonePub(page: Int = 1)      extends PubCase

trait SubCase:
  def data: String

object SubCase:
  case class txSub(data: String)            extends SubCase
  case class blockSub(data: String)         extends SubCase
  case class txDetailSub(data: String)      extends SubCase
  case class accountDetailSub(data: String) extends SubCase
  case class nftDetailSub(data: String)     extends SubCase
  case class blockDetailSub(data: String)   extends SubCase
  case class NoneSub(data: String = "")     extends SubCase

trait PageCase:
  def name: String
  def url: String
  def pubs: List[PubCase]
  def subs: List[SubCase]

object PageCase:
  case class Blocks(
      name: String = "Blocks",
      url: String = "Blocks",
      pubs: List[PubCase] = List(PubCase.blockPub(1)),
      subs: List[SubCase] = List(SubCase.NoneSub()),
  ) extends PageCase

  // case class DashBoard(
  //     name: String = "DashBoard",
  //     url: String = "DashBoard",
  //     pubs: List[PubCase] = List(PubCase.txPub(1), PubCase.blockPub(1)),
  //     subs: List[SubCase],
  // ) extends PageCase

  // case class Transactions(
  //     name: String = "Transactions",
  //     url: String = "Transactions",
  //     pubs: List[PubCase] = List(PubCase.txPub(1)),
  //     subs: List[SubCase],
  // ) extends PageCase

  // case class Observer(name: String = "Observer", url: String = "Observer")
  //     extends PageCase2

  // case class NoPage(name: String = "noPage", url: String = "noPage")
  //     extends PageCase2
