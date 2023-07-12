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
import io.leisuremeta.chain.lmscan.frontend.Log.log
import io.leisuremeta.chain.lmscan.frontend.StateCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.PageCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*

object Body:
  def blocks = (payload: List[BlockInfo]) =>
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
    model.appStates.map(state =>
      div()(
        div(
          `class` := s"row table-body ${state.number == get_latest_number(model)}_state ${model.pointer == state.number}_state_click",
          onClick(PageMsg.GotoObserver(state.number)),
        )(
          // #
          div(
            `class` := s"cell type-3 ",
          )(
            span()(state.number.toString()),
          ),
          // name
          div(`class` := "cell type-3")(
            span()(
              in_Name(state.pageCase),
            ),
          ),

          // url
          div(`class` := "cell")(
            span(
              in_url(state.pageCase),
            ),
          ),

          // pubs
          div(`class` := "cell")(
            span(
              in_PubCases(state.pageCase).length.toString(),
            ),
          ),

          // :page
          div(`class` := "cell")(
            span(
              // pipe_PubCase_Page(state.pageCase).toString(),
            ),
          ),

          // : PubCases |> map - pipe_PubCase_Page |> reduce
          // div(`class` := "cell")(
          //   span(
          //     pipe_PageCase_PubCase__Page_All(state.pageCase).toString(),
          //   ),
          // ),

          // : pub_m2
          // div(`class` := "cell")(
          //   span(
          //     pipe_PageCase_PubCase__pub_m1_All(state.pageCase).toString(),
          //   ),
          // ),
        ),
      ),
    )
  def txlist_txtable_off = (payload: List[TxInfo]) =>
    payload
      // List(new TxInfo)
      .map(v =>
        div(`class` := "row table-body")(
          gen.cell(
            Cell.TX_HASH10(v.hash),
            Cell.PlainLong(v.blockNumber),
            Cell.AGE(v.createdAt),
            Cell.ACCOUNT_HASH(v.signer),
            Cell.Tx_VALUE((v.subType, V.validNull(v.value))),
          ),
          // Cell.PlainInt(v.blockNumber),
          // Cell.PlainStr(v.txType),
          // Cell.PlainStr(v.subType),
        ),
      )
  def txlist_txtable_on = (payload: List[TxInfo]) =>
    payload
      // List(new TxInfo)
      .map(v =>
        div(`class` := "row table-body")(
          gen.cell(
            Cell.TX_HASH10(v.hash),
            Cell.PlainLong(v.blockNumber),
            Cell.AGE(v.createdAt),
            Cell.ACCOUNT_HASH(v.signer),
            Cell.PlainStr(v.subType), // subtype 추가
            Cell.Tx_VALUE((v.subType, V.validNull(v.value))),
          ),
          // Cell.PlainInt(v.blockNumber),
          // Cell.PlainStr(v.txType),
          // Cell.PlainStr(v.subType),
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

  def nft = (payload: List[NftActivity]) =>
    payload
      .map(v =>
        div(
          `class` := "row table-body",
        )(
          gen.cell(
            Cell.TX_HASH(v.txHash),
            Cell.AGE(v.createdAt),
            Cell.PlainStr(v.action),
            Cell.ACCOUNT_HASH(v.fromAddr),
            Cell.ACCOUNT_HASH(v.toAddr),
          ),
        ),
      )
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
            // Cell.PlainStr(v.subType),
            Cell.Tx_VALUE((v.subType, V.validNull(v.value))),
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
            // Cell.PlainStr(v.subType),
            Cell.Tx_VALUE2(
              (
                v.subType,
                V.validNull(v.value),
                v.inOut,
              ),
            ),
          ),
        ),
      )
