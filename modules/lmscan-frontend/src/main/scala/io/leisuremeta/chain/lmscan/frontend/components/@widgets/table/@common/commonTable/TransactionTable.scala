// package io.leisuremeta.chain.lmscan.frontend
// import tyrian.Html.*
// import tyrian.*
// import io.circe.*, io.circe.parser.*, io.circe.generic.semiauto.*
// import io.circe.syntax.*

// object Row2:

//   def sample_tx = """{
//             "hash": "7913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2dc",
//             "txType": "account",
//             "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
//             "toAddr": [
//                 "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
//             ],
//             "amount": 1.2345678912345679E8,
//             "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
//             "eventTime": 1673939878,
//             "createdAt": 21312412
//         }"""

//   def sample_tx_list = """[{
//             "hash": "7913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2dc",
//             "txType": "account",
//             "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
//             "toAddr": [
//                 "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
//             ],
//             "amount": 1.2345678912345679E8,
//             "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
//             "eventTime": 1673939878,
//             "createdAt": 21312412
//         }]"""

//   def title = (model: Model) =>
//     div(
//       `class` := s"${State.curPage(model, NavMsg.DashBoard: Msg, "_table-title")} table-title ",
//     )(
//       div(
//         `class` := s"type-1",
//       )(span()("Latest transactions")),
//       div(
//         `class` := s"type-2",
//       )(span(onClick(NavMsg.Transactions))("More")),
//     )
//   val head = div(`class` := "row table-head")(
//     div(`class` := "cell")(span()("TX Hash")),
//     div(`class` := "cell")(span()("Block")),
//     div(`class` := "cell")(span()("Age")),
//     div(`class` := "cell")(span()("Signer")),
//     div(`class` := "cell")(span()("Type")),
//     div(`class` := "cell")(span()("Token Type")),
//     div(`class` := "cell")(span()("Value")),
//   )
//   val body_old = div(`class` := "row table-body")(
//     div(`class` := "cell type-3")(
//       span(onClick(NavMsg.TransactionDetail))("bcf186a5ed..."),
//     ),
//     div(`class` := "cell")(span()("123,456,789")),
//     div(`class` := "cell")(span()("5s ago")),
//     div(`class` := "cell type-3")(
//       span(onClick(NavMsg.Account))("73c7e699d9..."),
//     ),
//     div(`class` := "cell")(span()("Account")),
//     div(`class` := "cell")(span()("NFT")),
//     div(`class` := "cell type-3")(span(onClick(NavMsg.Nft))("123,12412123 LM")),
//   )

//   val body_new = bodyGen(parse(sample_tx).getOrElse(Json.Null))

//   def bodyGen = (tx: Json) =>
//     div(`class` := "row table-body")(
//       div(`class` := "cell type-3")(
//         span(onClick(NavMsg.TransactionDetail))(
//           tx.hcursor.downField("hash").as[String].getOrElse("asd"),
//         ),
//       ),
//       div(`class` := "cell")(span()("123,456,789")),
//       div(`class` := "cell")(span()("5s ago")),
//       div(`class` := "cell type-3")(
//         span(onClick(NavMsg.Account))("73c7e699d9..."),
//       ),
//       div(`class` := "cell")(span()("Account")),
//       div(`class` := "cell")(span()("NFT")),
//       div(`class` := "cell type-3")(
//         span(onClick(NavMsg.Nft))("123,12412123 LM"),
//       ),
//     )
//   def body_result = () => bodyGen(parse(sample_tx).getOrElse(Json.Null))

// object TestRow:
//   val intJson = List(1, 2, 3).asJson

// case class Foo(a: Int, b: String, c: Boolean)

// object TransactionTable:

//   implicit val fooDecoder: Decoder[Foo] = deriveDecoder[Foo]
//   implicit val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]

//   def view(model: Model): Html[Msg] =
//     Log.log(TestRow.intJson)
//     Log.log(TestRow.intJson.as[List[Int]])
//     Log.log("fooDecoder")
//     Log.log(fooDecoder)
//     Log.log("fooEncoder")
//     Log.log(fooEncoder)
//     div(`class` := "table-container")(
//       Row2.title(model),
//       div(`class` := "table w-[100%]")(
//         Row2.head,
//         Row2.body_old,
//         Row2.body_old,
//         Row2.body_old,
//         Row2.body_old,
//         Row2.body_old,
//         Row2.body_old,
//       ),
//       Row.search(model),
//     )

package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.circe.*, io.circe.parser.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import Dom.{_hidden}

import Log.*

object Row2:
  def title = (model: Model) =>
    div(
      `class` := s"${State.curPage(model, NavMsg.DashBoard: Msg, "_table-title")} table-title ",
    )(
      div(
        `class` := s"type-1",
      )(span()("Latest transactions")),
      div(
        `class` := s"type-2",
      )(span(onClick(NavMsg.Transactions))("More")),
    )
  val head = div(`class` := "row table-head")(
    div(`class` := "cell")(span()("hash")),
    div(`class` := "cell")(span()("txType")),
    div(`class` := "cell")(span()("fromAddr")),
    div(`class` := "cell")(span()("toAddr")),
    div(`class` := "cell")(span()("amount")),
    div(`class` := "cell")(span()("blockHash")),
    div(`class` := "cell")(span()("eventTime")),
    div(`class` := "cell")(span()("createdAt")),
  )
  val body_old = div(`class` := "row table-body")(
    div(`class` := "cell type-3")(
      span(onClick(NavMsg.TransactionDetail))("bcf186a5ed..."),
    ),
    div(`class` := "cell")(span()("123,456,789")),
    div(`class` := "cell")(span()("5s ago")),
    div(`class` := "cell type-3")(
      span(onClick(NavMsg.Account))("73c7e699d9..."),
    ),
    div(`class` := "cell")(span()("Account")),
    div(`class` := "cell")(span()("NFT")),
    div(`class` := "cell type-3")(span(onClick(NavMsg.Nft))("123,12412123 LM")),
  )
  val search = (model: Model) =>
    div(
      `class` := s"${State.curPage(model, NavMsg.DashBoard, "_search")} table-search xy-center ",
    )(
      div(`class` := "xy-center")(
        div(
          `class` := s"type-arrow ${_hidden[Int](1, model.tx_CurrentPage)}",
          onClick(PageMoveMsg.Patch("1")),
        )("<<"),
        div(
          `class` := s"type-arrow ${_hidden[Int](1, model.tx_CurrentPage)}",
          onClick(PageMoveMsg.Prev),
        )("<"),
        div(`class` := "type-plain-text")("Page"),
        input(
          onInput(s => PageMoveMsg.Get(s)),
          value   := s"${model.tx_list_Search}",
          `class` := "type-search xy-center DOM-page1 ",
        ),
        div(`class` := "type-plain-text")("of"),
        div(`class` := "type-plain-text")(model.tx_TotalPage.toString()),
        div(
          `class` := s"type-arrow ${_hidden[Int](model.tx_TotalPage, model.tx_CurrentPage)}",
          onClick(PageMoveMsg.Next),
        )(">"),
        div(
          `class` := s"type-arrow ${_hidden[Int](model.tx_TotalPage, model.tx_CurrentPage)}",
          onClick(PageMoveMsg.Patch(model.tx_TotalPage.toString())),
        )(">>"),
      ),
    )

  // val body_new = bodyGen(parse(sample_tx).getOrElse(Json.Null))

  // def bodyGen = (tx: Json) =>
  //   div(`class` := "row table-body")(
  //     div(`class` := "cell type-3")(
  //       span(onClick(NavMsg.TransactionDetail))(
  //         tx.hcursor.downField("hash").as[String].getOrElse("asd"),
  //       ),
  //     ),
  //     div(`class` := "cell")(span()("123,456,789")),
  //     div(`class` := "cell")(span()("5s ago")),
  //     div(`class` := "cell type-3")(
  //       span(onClick(NavMsg.Account))("73c7e699d9..."),
  //     ),
  //     div(`class` := "cell")(span()("Account")),
  //     div(`class` := "cell")(span()("NFT")),
  //     div(`class` := "cell type-3")(
  //       span(onClick(NavMsg.Nft))("123,12412123 LM"),
  //     ),
  //   )
  def body = (payload: List[Tx]) =>
    payload
      .map(each =>
        div(`class` := "row table-body")(
          div(`class` := "cell")(span()(each.hash)),
          div(`class` := "cell")(span()(each.blockNumber.toString())),
          div(`class` := "cell")(span()(each.createdAt.toString())),
          div(`class` := "cell")(span()(each.txType)),
          div(`class` := "cell")(span()(each.tokenType)),
          div(`class` := "cell")(span()(each.signer)),
          div(`class` := "cell")(span()(each.value.toString())),
        ),
      )
      .toList
  def table = (payload: List[Tx]) =>
    div(`class` := "table w-[100%]")(
      Row2.head :: Row2.body(payload),
    )

object TransactionTable:
  def view(model: Model): Html[Msg] =
    val table = TxParser
      .decodeParser(model.data.get)
      .map(x => Row2.table(x.payload))
      .getOrElse(div())
    div(`class` := "table-container")(
      Row2.title(model),
      table,
      Row.search(model),
    )
