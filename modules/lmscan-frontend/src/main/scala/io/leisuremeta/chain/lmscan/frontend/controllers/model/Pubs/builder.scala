package io.leisuremeta.chain.lmscan.frontend

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
