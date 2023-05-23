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
    detail_button: Boolean = false,
    tx_total_page: String = "1",
    tx_current_page: String = "1",
    block_total_page: String = "1",
    block_current_page: String = "1",
)
