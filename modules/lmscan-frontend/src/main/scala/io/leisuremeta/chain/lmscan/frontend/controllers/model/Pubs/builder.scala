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

// model.observers
// |> find_Observer => observer
// |> in_Observer_PageCase => pageCase
// |> in_PageCase_pubs => pubs

// Observers
// - find_Observer

// observer
// - in_Observer_PageCase
// - in_Observer_Number // 없음

// PageCase(name,url,pubs,status)
// - in_PageCase_Name
// - in_PageCase_url
// - in_PageCase_pubs
// - in_PageCase_status // 없음

// PubCase(::page,pub_m1,pub_m2)
// - :: in_PubCase_page
// - :: in_PubCase_pub_m1
// - :: in_PubCase_pub_m2

// TODO:: go, pipe 함수로 redesign!
object Builder:
  // #1-observer
  def find_Observer(observers: List[ObserverState], find: Int) =
    // find 가 0이면 가장 최신 옵져버로 검색할수 있게 해준다
    val _find = find match
      case 0 => observers.length
      case _ => find
    observers.filter(o => o.number == _find)(0)

  // #1-observer-function
  def in_Observer_PageCase(observers: List[ObserverState], find: Int = 0) =
    find_Observer(observers, find).pageCase

  def in_Observer_Number(observers: List[ObserverState], find: Int = 0) =
    find_Observer(observers, find).number

  // #2-PageCase-function
  def in_PageCase_Name(pageCase: PageCase) =
    pageCase match
      case PageCase.Blocks(name, _, _, _)       => name
      case PageCase.Transactions(name, _, _, _) => name

  def in_PageCase_url(pageCase: PageCase) =
    pageCase match
      case PageCase.Blocks(_, url, _, _)       => url
      case PageCase.Transactions(_, url, _, _) => url

  def in_PageCase_pubs(pageCase: PageCase): List[PubCase] =
    pageCase match
      case PageCase.Blocks(_, _, pubs, _)       => pubs
      case PageCase.Transactions(_, _, pubs, _) => pubs

  // #3-PubCase-function
  def in_PubCase_Page(pageCase: PageCase) =
    in_PageCase_pubs(pageCase)(0) match
      case PubCase.BlockPub(page, _, _) => page
      case PubCase.TxPub(page, _, _)    => page

  def pipe_observers_reduced_data(observers: List[ObserverState]) =
    in_PageCase_pubs(in_Observer_PageCase(observers)).reverse.map(d =>
      d match
        case PubCase.BlockPub(_, _, pub_m2) =>
          // get_M3r1(pub_m2.payload.toList)
          pub_m2.payload.toList

        case PubCase.TxPub(_, _, pub_m2) =>
          // get_M3r1(pub_m2.payload.toList)
          pub_m2.payload.toList,
    )

  def update_pagecase_pub(pageCase: PageCase, pub: PubCase) =
    pageCase match
      case PageCase.Blocks(_, _, _, _) =>
        PageCase.Blocks(
          pubs = in_PageCase_pubs(pageCase) ++ List(pub),
        )
      case PageCase.Transactions(_, _, _, _) =>
        PageCase.Transactions(
          pubs = in_PageCase_pubs(pageCase) ++ List(pub),
        )

  def pipe_pubcase_apiUrl(pub: PubCase) =
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

  // def get_M3r1(m3: List[TxInfo] | List[BlockInfo]) =
  //   m3 match
  //     case m3: List[TxInfo]    => M3R1(txInfo = m3)
  //     case m3: List[BlockInfo] => M3R1(blockInfo = m3)
