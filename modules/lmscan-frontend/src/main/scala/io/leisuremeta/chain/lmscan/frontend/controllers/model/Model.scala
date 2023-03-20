package io.leisuremeta.chain.lmscan.frontend

import io.circe.Json
import org.scalafmt.config.Newlines.ForceBeforeMultilineAssign.any

case class Datas(
    txData: String = "",
    blockData: String = "",
    apiData: String = "",
    txDetailData: String = "",
    blockDetailData: String = "",
    nftDetailData: String = "",
)

case class ObserverState(
    pageCase: PageCase,
    number: Int,
    data: String,
    datas: Datas = Datas(),
)

final case class Model(
    observers: List[ObserverState],
    observerNumber: Int,
    // blockListData: Option[String] = Some(""),
    // curPage: PageCase,
)
