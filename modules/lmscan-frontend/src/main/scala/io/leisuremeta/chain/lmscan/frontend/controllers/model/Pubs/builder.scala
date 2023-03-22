package io.leisuremeta.chain.lmscan.frontend
import scala.scalajs.js
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.common.model.TxInfo
import io.leisuremeta.chain.lmscan.common.model.BlockInfo
import io.getquill.parser.Unlifter.caseClass
// import io.leisuremeta.chain.lmscan.frontend.M3R1.blockInfo

case class M3R1(
    txInfo: List[TxInfo] = List(new TxInfo),
    blockInfo: List[BlockInfo] = List(new BlockInfo),
);

// 파이프라인

// getObservers
// - getObserver

// observer(최신)
// - getObserver_PageCase
// - getObserver_Number // 없음

// PageCase(name,url,pubs,status)
// - getPageCase_Name
// - getPageCase_url
// - getPageCase_pubs
// - getPageCase_status

// PubCase(::page,pub_m1,pub_m2)
// - :: get
// - :: get
// - :: get
// - :: get

object Builder:
  // observer(최신)
  // - getObserver_PageCase
  // - getObserver_Number // 없음

  def getObserver(observers: List[ObserverState], find: Int) =
    // find 가 0이면 가장 최신 옵져버로 검색할수 있게 해준다
    val _find = find match
      case 0 => observers.length
      case _ => find
    observers.filter(o => o.number == _find)(0)

  def getObserver_PageCase(observers: List[ObserverState], find: Int = 0) =
    getObserver(observers, find).pageCase

  def getObserver_Number(observers: List[ObserverState], find: Int = 0) =
    getObserver(observers, find).number

  // PageCase(name,url,pubs,status)
  // - getPageCase_Name
  // - getPageCase_url
  // - getPageCase_pubs
  // - getPageCase_status -- 없음

  def getPageCase_Name(pageCase: PageCase) =
    pageCase match
      case PageCase.Blocks(name, _, _, _)       => name
      case PageCase.Transactions(name, _, _, _) => name

  def getPageCase_url(pageCase: PageCase) =
    pageCase match
      case PageCase.Blocks(_, url, _, _)       => url
      case PageCase.Transactions(_, url, _, _) => url

  def getPageCase_pubs(pageCase: PageCase): List[PubCase] =
    pageCase match
      case PageCase.Blocks(_, _, pubs, _)       => pubs
      case PageCase.Transactions(_, _, pubs, _) => pubs

  // def get_M3r1(m3: List[TxInfo] | List[BlockInfo]) =
  //   m3 match
  //     case m3: List[TxInfo]    => M3R1(txInfo = m3)
  //     case m3: List[BlockInfo] => M3R1(blockInfo = m3)

  // TODO:: go, pipe 함수로 redesign!
  def updatePagePubs(observers: List[ObserverState]) =
    getPageCase_pubs(getObserver_PageCase(observers)).reverse.map(d =>
      d match
        case PubCase.BlockPub(_, _, pub_m2) =>
          // get_M3r1(pub_m2.payload.toList)
          pub_m2.payload.toList

        case PubCase.TxPub(_, _, pub_m2) =>
          // get_M3r1(pub_m2.payload.toList)
          pub_m2.payload.toList,
    )

  def updatePagePubs(pageCase: PageCase, pub: PubCase) =
    pageCase match
      case PageCase.Blocks(_, _, _, _) =>
        PageCase.Blocks(
          pubs = getPageCase_pubs(pageCase) ++ List(pub),
        )
      case PageCase.Transactions(_, _, _, _) =>
        PageCase.Transactions(
          pubs = getPageCase_pubs(pageCase) ++ List(pub),
        )

  def getPubUrl(pub: PubCase) =
    var base = js.Dynamic.global.process.env.BASE_API_URL
    pub match
      case PubCase.BlockPub(_, _, _) =>
        s"$base/block/list?pageNo=${(0).toString()}&sizePerRequest=10"

      case _ =>
        s"$base/block/list?pageNo=${(0).toString()}&sizePerRequest=10"

  def updatePub_m1m2(pub: PubCase, data: String) =
    pub match
      case PubCase.BlockPub(_, _, _) =>
        PubCase.BlockPub(
          pub_m1 = data,
          pub_m2 = BlockParser
            .decodeParser(data)
            .getOrElse(new PageResponse(0, 0, List())),
        )

      // case PubCase.BlockPub(_, _, _) =>
      //   PubCase.BlockPub(
      //     pub_m1 = data,
      //     pub_m2 = BlockParser
      //       .decodeParser(data)
      //       .getOrElse(new PageResponse(0, 0, List())),
      //   )
      case _ => PubCase.BlockPub(pub_m1 = data)
