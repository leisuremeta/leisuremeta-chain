package io.leisuremeta.chain.lmscan.frontend
import io.circe.Json

final case class Model(
    appStates: List[StateCase],
    curAppState: Int,
)
