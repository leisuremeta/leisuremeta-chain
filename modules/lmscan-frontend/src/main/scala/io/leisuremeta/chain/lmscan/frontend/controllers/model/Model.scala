package io.leisuremeta.chain.lmscan.frontend

import io.circe.Json
import org.scalafmt.config.Newlines.ForceBeforeMultilineAssign.any

case class ObserverState(pageCase: PageCase, number: Int, data: String)

final case class Model(
    observers: List[ObserverState],
    observerNumber: Int,
    // blockListData: Option[String] = Some(""),
    // curPage: PageCase,
)
