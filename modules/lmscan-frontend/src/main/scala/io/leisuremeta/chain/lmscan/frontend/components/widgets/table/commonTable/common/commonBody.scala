package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.circe.*, io.circe.parser.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import Dom.{_hidden, isEqGet, yyyy_mm_dd_time, timeAgo}
import V.*
import Builder.*
import io.leisuremeta.chain.lmscan.common.model.BlockInfo
import io.leisuremeta.chain.lmscan.common.model.TxInfo
import io.leisuremeta.chain.lmscan.common.model.NftActivity

import io.leisuremeta.chain.lmscan.common.model.BlockInfo
import io.leisuremeta.chain.lmscan.common.model.TxInfo
import io.leisuremeta.chain.lmscan.common.model.NftActivity
import io.leisuremeta.chain.lmscan.frontend.Builder.*
import io.leisuremeta.chain.lmscan.frontend.Log.log

object Body:
  def block = (payload: List[BlockInfo]) =>
    payload
      .map(v =>
        div(`class` := "row table-body")(
          gen.cell(
            // TODO FIX :: BLOCK_NUMBER,HASH,PlainLong 초기값 할당되지 않을경우, 에러난다
            Cell.BLOCK_NUMBER(v.hash, v.number),
            Cell.AGE(v.createdAt),
            Cell.BLOCK_HASH(v.hash),
            Cell.PlainLong(v.txCount),
          ),
        ),
      )
  def observer = (model: Model) =>
    model.observers.map(observer =>
      div()(
        div(
          `class` := s"row table-body ${observer.number == in_Observer_Number(
              model.observers,
              model.observers.length,
            )}_observer ${model.observerNumber == observer.number}_observer_click",
          onClick(PageMsg.GotoObserver(observer.number)),
        )(
          // #
          div(
            `class` := s"cell type-3 ",
          )(
            span()(observer.number.toString()),
          ),
          // name
          div(`class` := "cell type-3")(
            span()(
              in_PageCase_Name(observer.pageCase),
            ),
          ),

          // url
          div(`class` := "cell")(
            span(
              in_PageCase_url(observer.pageCase),
            ),
          ),

          // pubs
          div(`class` := "cell")(
            span(
              in_PageCase_PubCases(observer.pageCase).length.toString(),
            ),
          ),

          // :page
          div(`class` := "cell")(
            span(
              // pipe_PubCase_Page(observer.pageCase).toString(),
            ),
          ),

          // : PubCases |> map - pipe_PubCase_Page |> reduce
          div(`class` := "cell")(
            span(
              pipe_PageCase_PubCase__Page_All(observer.pageCase).toString(),
            ),
          ),

          // : pub_m2
          div(`class` := "cell")(
            span(
              pipe_PageCase_PubCase__pub_m1_All(observer.pageCase).toString(),
            ),
          ),
        ),
        div(
          `class` := s"${observer.number == in_Observer_Number(
              model.observers,
              model.observerNumber,
            ) match
              case true => ""
              case _    => "hidden"
            }",
          // `class` := s"row table-body ${observer.number == in_Observer_Number(
          //     model.observers,
          //     model.observers.length,
          //   )}_observer ${model.observerNumber == observer.number}_observer_click",
        )(PageView.view2(model)),
      ),
    )
  def txlist_txtable = (payload: List[TxInfo]) =>
    payload
      .map(v =>
        div(`class` := "row table-body")(
          gen.cell(
            Cell.TX_HASH10(v.hash),
            // Cell.PlainInt(v.blockNumber),
            Cell.PlainLong(v.blockNumber),
            Cell.AGE(v.createdAt),
            Cell.ACCOUNT_HASH(v.signer),
            // Cell.PlainStr(v.txType),
            // Cell.PlainStr(v.tokenType),
            Cell.Tx_VALUE((v.tokenType, v.value)),
          ),
        ),
      )
  def dashboard_txtable = (payload: List[TxInfo]) =>
    payload
      .map(v =>
        div(`class` := "row table-body")(
          gen.cell(
            Cell.TX_HASH10(v.hash),
            Cell.AGE(v.createdAt),
            Cell.ACCOUNT_HASH(v.signer),
          ),
        ),
      )

//   def nft = (payload: List[NftActivity]) =>
//     payload
//       .map(v =>
//         div(
//           `class` := "row table-body",
//         )(
//           gen.cell(
//             Cell.TX_HASH(v.txHash),
//             Cell.AGE(v.createdAt),
//             Cell.PlainStr(v.action),
//             Cell.ACCOUNT_HASH(v.fromAddr),
//             Cell.ACCOUNT_HASH(v.toAddr),
//           ),
//         ),
//       )
  def blockDetail_txtable = (payload: List[TxInfo]) =>
    payload
      .map(v =>
        div(`class` := "row table-body")(
          gen.cell(
            Cell.TX_HASH(v.hash),
            // Cell.PlainInt(v.blockNumber),
            Cell.PlainLong(v.blockNumber),
            Cell.AGE(v.createdAt),
            Cell.ACCOUNT_HASH(v.signer),
            // Cell.PlainStr(v.txType),
            // Cell.PlainStr(v.tokenType),
            Cell.Tx_VALUE((v.tokenType, v.value)),
          ),
        ),
      )

  def accountDetail_txtable = (payload: List[TxInfo]) =>
    payload
      .map(v =>
        div(`class` := "row table-body")(
          gen.cell(
            Cell.TX_HASH(v.hash),
            // Cell.PlainInt(v.blockNumber),
            Cell.PlainLong(v.blockNumber),
            Cell.AGE(v.createdAt),
            Cell.ACCOUNT_HASH(v.signer),
            // Cell.PlainStr(v.txType),
            // Cell.PlainStr(v.tokenType),
            Cell.Tx_VALUE2((v.tokenType, v.value, v.inOut)),
          ),
        ),
      )
