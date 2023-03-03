package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.common.model.BlockDetail
import io.leisuremeta.chain.lmscan.common.model.TxInfo

// case class BlockDetail(
//     hash: Option[String] = None,
//     parentHash: Option[String] = None,
//     number: Option[Int] = None,
//     timestamp: Option[Int] = None,
//     txCount: Option[Int] = None,
//     txs: Option[List[Tx]] = None,
// )

object BlockDetailParser:
  given txDecoder: Decoder[TxInfo] = deriveDecoder[TxInfo]
  given blockDetailDecoder: Decoder[BlockDetail] = deriveDecoder[BlockDetail]
  def decodeParser(body: String) = decode[BlockDetail](body)
