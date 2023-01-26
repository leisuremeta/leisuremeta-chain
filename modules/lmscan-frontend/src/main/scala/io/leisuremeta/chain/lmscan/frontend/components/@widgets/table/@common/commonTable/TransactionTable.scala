package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.circe.*, io.circe.parser.*
object Row2:

  def sample_tx = """{
            "hash": "7913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2dc",
            "txType": "account",
            "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
            "toAddr": [
                "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
            ],
            "amount": 1.2345678912345679E8,
            "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "eventTime": 1673939878,
            "createdAt": 21312412
        }"""

  def sample_tx_list = """[{
            "hash": "7913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2dc",
            "txType": "account",
            "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
            "toAddr": [
                "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
            ],
            "amount": 1.2345678912345679E8,
            "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "eventTime": 1673939878,
            "createdAt": 21312412
        }]"""

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
    div(`class` := "cell")(span()("TX Hash")),
    div(`class` := "cell")(span()("Block")),
    div(`class` := "cell")(span()("Age")),
    div(`class` := "cell")(span()("Signer")),
    div(`class` := "cell")(span()("Type")),
    div(`class` := "cell")(span()("Token Type")),
    div(`class` := "cell")(span()("Value")),
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

  val body_new = bodyGen(parse(sample_tx).getOrElse(Json.Null))

  def bodyGen = (tx: Json) =>
    div(`class` := "row table-body")(
      div(`class` := "cell type-3")(
        span(onClick(NavMsg.TransactionDetail))(
          tx.hcursor.downField("hash").as[String].getOrElse("asd"),
        ),
      ),
      div(`class` := "cell")(span()("123,456,789")),
      div(`class` := "cell")(span()("5s ago")),
      div(`class` := "cell type-3")(
        span(onClick(NavMsg.Account))("73c7e699d9..."),
      ),
      div(`class` := "cell")(span()("Account")),
      div(`class` := "cell")(span()("NFT")),
      div(`class` := "cell type-3")(
        span(onClick(NavMsg.Nft))("123,12412123 LM"),
      ),
    )
  def body_result = () => bodyGen(parse(sample_tx).getOrElse(Json.Null))

object TransactionTable:
  def view(model: Model): Html[Msg] =
    // Log.log("Row2.body_result()")
    // Log.log(Row2.body_result())
    div(`class` := "table-container")(
      Row2.title(model),
      div(`class` := "table w-[100%]")(
        Row2.head,
        Row2.body_old,
        Row2.body_old,
        Row2.body_old,
        Row2.body_old,
        Row2.body_old,
        Row2.body_old,
      ),
      Row.search(model),
    )
