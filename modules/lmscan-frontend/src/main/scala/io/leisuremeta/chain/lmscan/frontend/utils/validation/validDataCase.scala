package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.frontend.Log.log

object DataBuilder:
  def getNew(observers: List[ObserverState], find: Int) =
    log(observers.filter(o => o.number == find))
    observers.takeRight(1)(0)
    observers.filter(o => o.number == find)(0)
  // def getPage(observers: List[ObserverState], find: Int) =
  //   // 최신 상태에서 page 를 만듦
  //   getNew(observers, find).pageCase
  // def getNumber(observers: List[ObserverState], find: Int) =
  //   // 최신 상태에서 page 를 만듦
  //   getNew(observers, find).number

  // def getData(observers: List[ObserverState], find: Int) =
  //   // 최신 상태에서 page 를 만듦
  //   getNew(observers, find).data

trait DataCase:
  def page: Int
  def data: String

  // def page: Int

object DataCase:

  case class txData(page: Int = 1, data: String = "")    extends DataCase
  case class blockData(page: Int = 1, data: String = "") extends DataCase

  // case class txDetailData(page: Int = 1, data: String = "") extends DataCase
  // case class nftDetailData(page: Int = 1, data: String = "") extends DataCase
  // case class txData(page: Int = 1, data: String = "") extends DataCase
  // case class txData(page: Int = 1, data: String = "") extends DataCase
