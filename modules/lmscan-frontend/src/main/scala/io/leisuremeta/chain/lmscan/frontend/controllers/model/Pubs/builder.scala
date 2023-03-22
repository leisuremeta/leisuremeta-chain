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

  def getPageName(pageCase: PageCase) =
    pageCase match
      case PageCase.Blocks(name, _, _, _)       => name
      case PageCase.Transactions(name, _, _, _) => name

  def getPageUrl(pageCase: PageCase) =
    pageCase match
      case PageCase.Blocks(_, url, _, _)       => url
      case PageCase.Transactions(_, url, _, _) => url

  def getPagePubs(pageCase: PageCase) =
    pageCase match
      case PageCase.Blocks(_, _, pubs, _)       => pubs
      case PageCase.Transactions(_, _, pubs, _) => pubs

  def updatePagePubs(pageCase: PageCase, pub: PubCase) =
    pageCase match
      case PageCase.Blocks(_, _, _, _) =>
        PageCase.Blocks(
          pubs = getPagePubs(pageCase) ++ List(pub),
        )
      case PageCase.Transactions(_, _, _, _) =>
        PageCase.Transactions(
          pubs = getPagePubs(pageCase) ++ List(pub),
        )
