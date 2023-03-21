package io.leisuremeta.chain.lmscan.frontend
import io.leisuremeta.chain.lmscan.common.model.*

trait PubCase:
  def page: Int
  def pub_m1: String
  def pub_m2: PageResponse[BlockInfo]

object PubCase:

  case class blockPub(
      page: Int = 1,
      pub_m1: String = "",
      pub_m2: PageResponse[BlockInfo] =
        new PageResponse[BlockInfo](0, 0, List()),
  ) extends PubCase

  // case class txPub(page: Int, pub_m1: String, pub_m2: PageResponse[TxInfo])
  //     extends PubCase
  // case class txDetailPub(page: Int)      extends PubCase
  // case class accountDetailPub(page: Int) extends PubCase
  // case class nftDetailPub(page: Int)     extends PubCase
  // case class blockDetailPub(page: Int)   extends PubCase
  // case class NonePub(page: Int = 1)      extends PubCase

  // package io.leisuremeta.chain.lmscan.frontend

// import tyrian.Html.*
// import tyrian.*
// import io.circe.*, io.circe.parser.*, io.circe.generic.semiauto.*
// import io.circe.syntax.*
// import Dom.{_hidden, timeAgo, yyyy_mm_dd_time}
// import V.*

// import Log.*
// import io.leisuremeta.chain.lmscan.common.model.*
// import io.leisuremeta.chain.lmscan.frontend.Builder.getPage

// object DataProcess:
//   // val getDataNew = (model: Model) =>
//   //   Builder.getData(model.observers, model.observerNumber)

//   // def block(model: Model) =
//   //   val data: PageResponse[BlockInfo] = BlockParser
//   //     .decodeParser(getDataNew(model))
//   //     .getOrElse(new PageResponse(0, 0, List()))

//   //   data.payload.toList

//   // def board(model: Model) =
//   //   val data: SummaryModel =
//   //     ApiParser
//   //       .decodeParser(getDataNew(model))
//   //       .getOrElse(new SummaryModel)
//   //   data

//   // def dashboard_tx(model: Model) =
//   //   val data: PageResponse[TxInfo] = TxParser
//   //     .decodeParser(getDataNew(model))
//   //     .getOrElse(new PageResponse(0, 0, List()))
//   //   data.payload.toList

// // def common(model: Model) =
// //   // 현재 상태의 페이지를 가져온다
// //   val observedPage = getPage(model.observers, model.observerNumber)
// //   // 현재 상태의 데이터를 가져온다
// //   val observedData = getData(model.observers, model.observerNumber)

// //   val processedData = observedPage match
// //     case PageCase.Blocks(_, _, _) =>
// //       BlockParser
// //         .decodeParser(
// //           Builder.getData(model.observers, model.observers.length),
// //         )
// //         .getOrElse(new PageResponse(0, 0, List()))

// //     case PageCase.Transactions(_, _) =>
// //       TxParser
// //         .decodeParser(
// //           Builder.getData(model.observers, model.observers.length),
// //         )
// //         .getOrElse(new PageResponse(0, 0, List()))

// //     case _ =>
// //       BlockParser
// //         .decodeParser(
// //           Builder.getData(model.observers, model.observers.length),
// //         )
// //         .getOrElse(new PageResponse(0, 0, List()))

// //   processedData.payload.toList

// //   def nft(model: Model) =
// //     // val data: NftDetail = NftDetailParser.decodeParser(model.nftDetailData.get).getOrElse(new NftDetail)
// //     // val payload = getOptionValue(data.activities, List()).asInstanceOf[List[NftActivities]]
// //     // payload
// //     val data: NftDetail = NftDetailParser
// //       .decodeParser(model.nftDetailData.get)
// //       .getOrElse(new NftDetail)
// //     getOptionValue(data.activities, List()).asInstanceOf[List[NftActivity]]

// //   def blockDetail_tx(model: Model) =
// //     // val data: BlockDetail = BlockDetailParser.decodeParser(model.blockDetailData.get).getOrElse(new BlockDetail)
// //     // val payload = getOptionValue(data.txs, List()).asInstanceOf[List[Tx]]
// //     // payload
// //     val data: BlockDetail = BlockDetailParser
// //       .decodeParser(model.blockDetailData.get)
// //       .getOrElse(new BlockDetail)
// //     getOptionValue(data.txs, List()).asInstanceOf[List[TxInfo]]

// //   def acountDetail_tx(model: Model) =
// //     // val data: AccountDetail = AccountDetailParser.decodeParser(model.accountDetailData.get).getOrElse(new AccountDetail)
// //     // val payload = getOptionValue(data.txHistory, List()).asInstanceOf[List[Tx]]
// //     // payload
// //     val data: AccountDetail = AccountDetailParser
// //       .decodeParser(model.accountDetailData.get)
// //       .getOrElse(new AccountDetail)
// //     getOptionValue(data.txHistory, List()).asInstanceOf[List[TxInfo]]
