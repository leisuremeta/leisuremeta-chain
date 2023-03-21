package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.circe.*, io.circe.parser.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import Dom.{_hidden, isEqGet, yyyy_mm_dd_time, timeAgo}
import V.*
import io.leisuremeta.chain.lmscan.common.model.BlockInfo
import io.leisuremeta.chain.lmscan.common.model.TxInfo
import io.leisuremeta.chain.lmscan.common.model.NftActivity

import io.leisuremeta.chain.lmscan.common.model.BlockInfo
import io.leisuremeta.chain.lmscan.common.model.TxInfo
import io.leisuremeta.chain.lmscan.common.model.NftActivity
import io.leisuremeta.chain.lmscan.frontend.Builder.getNumber
import io.leisuremeta.chain.lmscan.frontend.Log.log

object Body:
  def block = (payload: List[BlockInfo]) =>
    payload
      .map(v =>
        div(`class` := "row table-body")(
          gen.cell(
            Cell.BLOCK_NUMBER(v.hash, v.number),
            Cell.AGE(v.createdAt),
            Cell.BLOCK_HASH(v.hash),
            Cell.PlainLong(v.txCount),
          ),
        ),
      )
  def observer = (model: Model) =>
    model.observers.map(observer =>
      div(
        `class` := s"row table-body ${observer.number == getNumber(
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
          span()(observer.pageCase.name),
        ),

        // url
        div(`class` := "cell")(span(observer.pageCase.url)),

        // :page
        div(`class` := "cell")(span(observer.pageCase.pubs(0).page.toString())),

        // : pub_m1
        div(`class` := "cell")(
          span(
            observer.pageCase.pubs.takeRight(1)(0).pub_m1.toString().take(150),
          ),
        ),

        // : pub_m2
        div(`class` := "cell")(
          span(
            observer.pageCase.pubs.takeRight(1)(0).pub_m2.toString().take(150),
          ),
        ),

        // div(`class` := "cell")(span(observer.pageCase.pubs(0).)),
        // div(`class` := "cell")(span(observer.pageCase.pubs(0).page)),
        // div(`class` := "cell")(span(observer.pageCase..toString())),
        // div(`class` := "cell")(span(observer.pageCase.pubs.toString())),
        // div(`class` := "cell")(span(observer.pageCase.status.toString())),
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
//   def blockDetail_txtable = (payload: List[TxInfo]) =>
//     payload
//       .map(v =>
//         div(`class` := "row table-body")(
//           gen.cell(
//             Cell.TX_HASH(v.hash),
//             // Cell.PlainInt(v.blockNumber),
//             Cell.PlainLong(v.blockNumber),
//             Cell.AGE(v.createdAt),
//             Cell.ACCOUNT_HASH(v.signer),
//             // Cell.PlainStr(v.txType),
//             // Cell.PlainStr(v.tokenType),
//             Cell.Tx_VALUE((v.tokenType, v.value)),
//           ),
//         ),
//       )

//   def accountDetail_txtable = (payload: List[TxInfo]) =>
//     payload
//       .map(v =>
//         div(`class` := "row table-body")(
//           gen.cell(
//             Cell.TX_HASH(v.hash),
//             // Cell.PlainInt(v.blockNumber),
//             Cell.PlainLong(v.blockNumber),
//             Cell.AGE(v.createdAt),
//             Cell.ACCOUNT_HASH(v.signer),
//             // Cell.PlainStr(v.txType),
//             // Cell.PlainStr(v.tokenType),
//             Cell.Tx_VALUE2((v.tokenType, v.value, v.inOut)),
//           ),
//         ),
//       )
//   def dashboard_txtable = (payload: List[TxInfo]) =>
//     payload
//       .map(v =>
//         div(`class` := "row table-body")(
//           gen.cell(
//             Cell.TX_HASH10(v.hash),
//             Cell.AGE(v.createdAt),
//             Cell.ACCOUNT_HASH(v.signer),
//           ),
//         ),
//       )
