package io.leisuremeta.chain.lmscan.frontend
import io.circe.Json

case class DataCase(
    txData: String = "",
    blockData: String = "",
    apiData: String = "",
    txDetailData: String = "",
    blockDetailData: String = "",
    nftDetailData: String = "",
)
