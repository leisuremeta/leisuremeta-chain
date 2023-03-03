package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.common.model.AccountDetail
import io.leisuremeta.chain.lmscan.common.model.TxInfo

object AccountDetailParser:
  given accountDetailDecoder: Decoder[AccountDetail] =
    deriveDecoder[AccountDetail]
  given txDecoder: Decoder[TxInfo] = deriveDecoder[TxInfo]
  def decodeParser(body: String)   = decode[AccountDetail](body)
