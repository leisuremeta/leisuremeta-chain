package io.leisuremeta.chain.lmscan.frontend
import io.circe.Json

final case class Model(
    appStates: List[StateCase],
    pointer: Int = 1,
    searchValue: String = "",
    toggle: Boolean = false,
    temp: String = "",
    commandMode: CommandCaseMode = CommandCaseMode.Production,
    commandLink: CommandCaseLink,
    detail_button: Boolean = false,
    tx_total_page: String = "1",
    tx_current_page: String = "1",
    subtype: String = "",
    block_total_page: String = "1",
    block_current_page: String = "1",
    pageLimit: Int = 50,
    popup: Boolean = false,
    lmprice: Double = 0.0,
)
