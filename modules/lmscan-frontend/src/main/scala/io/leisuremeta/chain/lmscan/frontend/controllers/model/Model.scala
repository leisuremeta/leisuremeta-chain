package io.leisuremeta.chain.lmscan.frontend

import io.circe.Json
import org.scalafmt.config.Newlines.ForceBeforeMultilineAssign.any

final case class Model(
    observer: List[Any],
    curPage: PageName,
)
