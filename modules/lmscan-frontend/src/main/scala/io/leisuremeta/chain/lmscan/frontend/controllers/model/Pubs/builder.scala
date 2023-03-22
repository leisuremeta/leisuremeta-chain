package io.leisuremeta.chain.lmscan.frontend
import scala.scalajs.js
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.common.model.TxInfo
import io.leisuremeta.chain.lmscan.common.model.BlockInfo
import io.getquill.parser.Unlifter.caseClass
import io.leisuremeta.chain.lmscan.frontend.Log.log

case class ViewCase(
    var blockInfo: List[BlockInfo] = List(new BlockInfo),
    var txInfo: List[TxInfo] = List(new TxInfo),
);

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

  def in_PageCase_PubCases(pageCase: PageCase): List[PubCase] =
    pageCase match
      case PageCase.Blocks(_, _, pubs, _)       => pubs
      case PageCase.Transactions(_, _, pubs, _) => pubs

  // #3-PubCase-function
  def in_PubCase_Page(pubCase: PubCase) =
    pubCase match
      case PubCase.BlockPub(page, _, _) => page
      case PubCase.TxPub(page, _, _)    => page

  def in_PubCase_pub_m1(pubCase: PubCase) =
    pubCase match
      case PubCase.BlockPub(_, pub_m1, _) => pub_m1
      case PubCase.TxPub(_, pub_m1, _)    => pub_m1

  def in_PubCase_pub_m2(pubCase: PubCase) =
    pubCase match
      case PubCase.BlockPub(_, _, pub_m2) => pub_m2
      case PubCase.TxPub(_, _, pub_m2)    => pub_m2

  // # PageCase |> [Pubcase] |> [in page] |> all page
  def pipe_PageCase_PubCase__Page_All(pageCase: PageCase) =
    in_PageCase_PubCases(pageCase)
      .map(d => in_PubCase_Page(d))
      .reduce((a, b) => a + b)

  // # PageCase |> [Pubcase] |> [in pub_m1] |> all page
  def pipe_PageCase_PubCase__pub_m1_All(pageCase: PageCase) =
    in_PageCase_PubCases(log(pageCase))
      .map(d => in_PubCase_pub_m1(d))
      .reduce((a, b) => a + b)

  // # PageCase |> [Pubcase] |> [pub_m2] |> ViewCase(tx,block,.....)
  def pipe_PageCase_ViewCase(pageCase: PageCase): ViewCase =

    // ViewCase 재할당이 일어나는 구조이므로, 리팩토링 필요할듯
    var resulte = new ViewCase()
    in_PageCase_PubCases(pageCase)
      .map(d =>
        d match
          case PubCase.BlockPub(_, _, pub_m2) =>
            resulte.blockInfo = pub_m2.payload.toList

          case PubCase.TxPub(_, _, pub_m2) =>
            resulte.txInfo = pub_m2.payload.toList,
          // case _ =>
          //   "no",
      )
    resulte

  def getViewCase(model: Model): ViewCase =
    pipe_PageCase_ViewCase(
      in_Observer_PageCase(model.observers, model.observerNumber),
    )

  //
  def update_PageCase_PubCases(pageCase: PageCase, pub: PubCase) =
    pageCase match
      case PageCase.Blocks(_, _, _, _) =>
        PageCase.Blocks(
          pubs = in_PageCase_PubCases(pageCase) ++ List(pub),
        )
      case PageCase.Transactions(_, _, _, _) =>
        PageCase.Transactions(
          pubs = in_PageCase_PubCases(pageCase) ++ List(pub),
        )

  def pipe_pubcase_apiUrl(pub: PubCase) =
    var base = js.Dynamic.global.process.env.BASE_API_URL
    pub match
      case PubCase.BlockPub(_, _, _) =>
        s"$base/block/list?pageNo=${(0).toString()}&sizePerRequest=10"

      case PubCase.TxPub(_, _, _) =>
        s"$base/tx/list?pageNo=${(0).toString()}&sizePerRequest=10"

      // case _ =>
      //   s"$base/block/list?pageNo=${(0).toString()}&sizePerRequest=10"

  def updatePub_m1m2(pub: PubCase, data: String) =
    pub match
      case PubCase.BlockPub(_, _, _) =>
        PubCase.BlockPub(
          pub_m1 = data,
          pub_m2 = BlockParser
            .decodeParser(data)
            .getOrElse(new PageResponse(0, 0, List())),
        )

      case PubCase.TxPub(_, _, _) =>
        PubCase.TxPub(
          pub_m1 = data,
          pub_m2 = TxParser
            .decodeParser(data)
            .getOrElse(new PageResponse(0, 0, List())),
        )
