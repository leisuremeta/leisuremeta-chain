// package io.leisuremeta.chain.lmscan.frontend
// import io.circe.*, io.circe.generic.semiauto.*
// import io.circe.syntax.*
// import io.circe.parser.*
// import io.leisuremeta.chain.lmscan.frontend.Log.log

// object Builder:
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

// trait PageCase:
//   def name: String
//   def url: String

//   // def page: Int

// object PageCase:

//   case class DashBoard(name: String = "DashBoard", url: String = "DashBoard")
//       extends PageCase

//   case class Observer(name: String = "Observer", url: String = "Observer")
//       extends PageCase

//   case class Blocks(
//       name: String = "Blocks",
//       url: String = "Blocks",
//       data: Option[String] = Some(""),
//   ) extends PageCase

//   case class Transactions(
//       name: String = "Transactions",
//       url: String = "Transactions",
//   ) extends PageCase

//   case class NoPage(name: String = "noPage", url: String = "noPage")
//       extends PageCase

// // case class Datas(
// //     txData: String = "",
// //     blockData: String = "",
// //     apiData: String = "",
// //     txDetailData: String = "",
// //     blockDetailData: String = "",
// //     nftDetailData: String = "",
// // )

// // case class ObserverState(
// //     pageCase: PageCase,
// //     number: Int,
// //     data: String,
// //     datas: Datas = Datas(),
// // )

// // final case class Model(
// //     observers: List[ObserverState],
// //     observerNumber: Int,
// //     // blockListData: Option[String] = Some(""),
// //     // curPage: PageCase,
// // )
