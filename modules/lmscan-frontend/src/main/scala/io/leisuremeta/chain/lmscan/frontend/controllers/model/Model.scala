package io.leisuremeta.chain.lmscan.frontend
import io.circe.Json

final case class Model(
    appStates: List[StateCase],
    pointer: Int,
    searchValue: String,
    toggle: Boolean,
    temp: String,
    commandMode: CommandCaseMode = CommandCaseMode.Production,
    commandLink: CommandCaseLink,
)
