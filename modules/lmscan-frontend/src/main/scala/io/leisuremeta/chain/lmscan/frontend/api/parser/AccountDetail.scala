package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.common.model.AccountDetail
import io.leisuremeta.chain.lmscan.common.model.TxInfo

// case class AccountDetail(
//     address: Option[String] = None,
//     balance: Option[Double] = None,
//     value: Option[Double] = None,
//     txHistory: Option[List[Tx]] = None,
// )

object AccountDetailParser:
  given accountDetailDecoder: Decoder[AccountDetail] =
    deriveDecoder[AccountDetail]
  given txDecoder: Decoder[TxInfo] = deriveDecoder[TxInfo]
  def decodeParser(body: String)   = decode[AccountDetail](body)
