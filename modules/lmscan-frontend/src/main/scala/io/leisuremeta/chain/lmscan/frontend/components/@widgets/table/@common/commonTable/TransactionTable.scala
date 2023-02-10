package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.circe.*, io.circe.parser.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import Dom.{_hidden, isEqGet, yyyy_mm_dd_time, timeAgo}
import ValidOutputData.*

import Log.*

// TODO :: simplify
object Row2:
  def title = (model: Model) =>
    div(
      `class` := s"${State.curPage(model, PageName.DashBoard, "_table-title")} table-title ",
    )(
      div(
        `class` := s"type-1",
      )(span()("Latest transactions")),
      div(
        `class` := s"type-2",
      )(
        span(
          onClick(PageMsg.PreUpdate(PageName.Transactions)),
        )("More"),
      ),
    )

  val head = div(`class` := "row table-head")(
    div(`class` := "cell")(span()("Tx Hash")), // hash
    div(`class` := "cell")(span()("Block")), // blockNumber
    div(`class` := "cell")(span()("Age")), // createdAt
    div(`class` := "cell")(span()("Signer")), // signer
    div(`class` := "cell")(span()("Type")), // txType
    div(`class` := "cell")(span()("Token Type")),
    div(`class` := "cell")(span()("Value")),
  )

  val headForDashboard = div(`class` := "row table-head")(
    div(`class` := "cell")(span()("Tx Hash")), // hash
    div(`class` := "cell")(span()("Age")), // createdAt
    div(`class` := "cell")(span()("Signer")), // signer
    div(`class` := "cell")(span()("Type")), // txType
  )

  def genBody = (payload: List[Tx]) =>
    payload
      .map(each =>
        div(`class` := "row table-body")(
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.TransactionDetail(
                    getOptionValue(each.hash, "-").toString(),
                  ),
                ),
              ),
            )(
              getOptionValue(each.hash, "-")
                .toString()
                .take(10) + "...",
            ),
          ),
          div(`class` := "cell")(
            span()(getOptionValue(each.blockNumber, "-").toString()),
          ),
          div(`class` := "cell")(
            span()(
              yyyy_mm_dd_time(
                getOptionValue(each.createdAt, 0).asInstanceOf[Int],
              ),
            ),
          ),
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.AccountDetail(
                    getOptionValue(
                      each.signer,
                      "-",
                    )
                      .toString(),
                  ),
                ),
              ),
            )(
              getOptionValue(each.signer, "-")
                .toString(),
                // .take(10) + "...",
            ),
          ),
          div(`class` := "cell")(
            span()(getOptionValue(each.txType, "-").toString()),
          ),
          div(`class` := "cell")(
            span()(getOptionValue(each.tokenType, "-").toString()),
          ),
          div(
            `class` := s"cell ${isEqGet[String](getOptionValue(each.tokenType, "-").toString(), "NFT", "type-3")}",
          )(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.NftDetail(
                    getOptionValue(each.value, "-").toString(),
                  ),
                ),
              ),
            )(
              getOptionValue(each.value, "-")
                .toString(),
                // .take(10) + "...",
            ),
          ),
        ),
      )

  def genBodyForAccountDetail = (payload: List[Tx]) =>
    payload.zipWithIndex
      .map { case (each: Tx, index: Int) =>
        val temp = index % 2 == 0
        div(`class` := "row table-body")(
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.TransactionDetail(
                    getOptionValue(each.hash, "-").toString(),
                  ),
                ),
              ),
            )(
              getOptionValue(each.hash, "-")
                .toString()
                .take(10) + "...",
            ),
          ),
          div(`class` := "cell")(
            span()(getOptionValue(each.blockNumber, "-").toString()),
          ),
          div(`class` := "cell")(
            span()(
              yyyy_mm_dd_time(
                getOptionValue(each.createdAt, 0).asInstanceOf[Int],
              ),
            ),
          ),
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.AccountDetail(
                    getOptionValue(
                      each.signer,
                      "-",
                    )
                      .toString(),
                  ),
                ),
              ),
            )(
              getOptionValue(each.signer, "-")
                .toString()
                .take(10) + "...",
            ),
          ),
          div(`class` := "cell")(
            span()(getOptionValue(each.txType, "-").toString()),
          ),
          div(`class` := "cell")(
            span()(getOptionValue(each.tokenType, "-").toString()),
          ),
          div(
            `class` := s"cell ${isEqGet[String](getOptionValue(each.tokenType, "-").toString(), "NFT", "type-3")}",
          )(
            span(
              temp match
                case true =>
                  List(
                    // onClick(NavMsg.Transactions),
                    style(
                      Style(
                        "background-color" -> "white",
                        "padding"          -> "5px",
                        "border"           -> "1px solid green",
                        "border-radius"    -> "5px",
                        "margin-right"     -> "5px",
                      ),
                    ),
                  )
                case false =>
                  List(
                    // onClick(NavMsg.Transactions),
                    style(
                      Style(
                        "background-color" -> "rgba(171, 242, 0, 0.5)",
                        "padding"          -> "5px",
                        "border"           -> "1px solid green",
                        "border-radius"    -> "5px",
                        "margin-right"     -> "5px",
                      ),
                    ),
                  ),
            )(getOptionValue(each.hash, "-").toString().take(5)),
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.NftDetail(
                    getOptionValue(each.value, "-").toString(),
                  ),
                ),
              ),
            )(
              getOptionValue(each.value, "-")
                .toString(),
                // .take(10) + "...",
            ),
          ),
        )
      }

  def genBodyForDashboard = (payload: List[Tx]) =>
    payload
      .map(each =>
        div(`class` := "row table-body")(
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.TransactionDetail(
                    getOptionValue(each.hash, "-").toString(),
                  ),
                ),
              ),
            )(
              getOptionValue(each.hash, "-")
                .toString()
                .take(10) + "...",
            ),
          ),
          div(`class` := "cell")(
            span()(
              timeAgo(
                getOptionValue(each.createdAt, 0).asInstanceOf[Int],
              ),
            ),
          ),
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  log(
                    PageName.AccountDetail(
                      getOptionValue(
                        each.signer,
                        "-",
                      )
                        .toString(),
                    ),
                  ),
                ),
              ),
            )(
              getOptionValue(each.signer, "-")
                .toString()
                .take(10) + "...",
            ),
          ),
          div(`class` := "cell")(
            span()(getOptionValue(each.txType, "-").toString()),
          ),
        ),
      )

  val search = (model: Model) =>
    div(
      `class` := s"${State.curPage(model, PageName.DashBoard, "_search")} table-search xy-center ",
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

  def genTable = (payload: List[Tx], model: Model) =>
    payload.isEmpty match
      case true =>
        model.curPage match
          case PageName.BlockDetail(_) => div()
          // case PageName.AccountDetail(_) => div()
          case _ =>
            div(`class` := "table-container")(
              Row2.title(model),
              div(`class` := "table w-[100%]")(
                Row2.head :: Row2.genBody(payload),
              ),
              // Row2.search(model),
            )
      case _ =>
        model.curPage match
          case PageName.BlockDetail(_) =>
            div(`class` := "table-container")(
              div(`class` := "table w-[100%]")(
                Row2.head :: Row2.genBody(payload),
              ),
            )
          case PageName.AccountDetail(_) =>
            div(`class` := "table-container")(
              div(`class` := "table w-[100%]")(
                Row2.head :: Row2.genBodyForAccountDetail(payload),
              ),
            )
          case PageName.DashBoard =>
            div(`class` := "table-container")(
              Row2.title(model),
              div(`class` := "table w-[100%]")(
                Row2.headForDashboard :: Row2.genBodyForDashboard(payload),
              ),
            )
          case _ =>
            div(`class` := "table-container")(
              Row2.title(model),
              div(`class` := "table w-[100%]")(
                Row2.head :: Row2.genBody(payload),
              ),
              Row2.search(model),
            )

  val txList_txtable = (model: Model) =>
    val data: TxList =
      TxParser.decodeParser(model.txListData.get).getOrElse(new TxList)
    val payload =
      getOptionValue(data.payload, List()).asInstanceOf[List[Tx]]
    Row2.genTable(payload, model)

  val account_txtable = (model: Model) =>
    val data: AccountDetail = AccountDetailParser
      .decodeParser(model.accountDetailData.get)
      .getOrElse(new AccountDetail)
    val txHistory =
      getOptionValue(data.txHistory, List()).asInstanceOf[List[Tx]]
    Row2.genTable(txHistory, model)

  val blockDetail_txtable = (model: Model) =>
    val data: BlockDetail = BlockDetailParser
      .decodeParser(model.blockDetailData.get)
      .getOrElse(new BlockDetail)
    val txs = getOptionValue(data.txs, List()).asInstanceOf[List[Tx]]
    Row2.genTable(txs, model)

object TransactionTable:
  def view(model: Model): Html[Msg] =
    model.curPage match
      case PageName.BlockDetail(_) =>
        Row2.blockDetail_txtable(model)

      case PageName.AccountDetail(_) =>
        Row2.account_txtable(model)

      case _ =>
        Row2.txList_txtable(model)
