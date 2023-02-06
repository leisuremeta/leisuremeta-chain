package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.parser.*

case class AccountDetail(
    address: String,
    balance: Double,
    value: Double,
    txHistory: List[Tx],
)

object AccountDetailParser:
  given accountDetailDecoder: Decoder[AccountDetail] =
    deriveDecoder[AccountDetail]
  given txDecoder: Decoder[Tx]   = deriveDecoder[Tx]
  def decodeParser(body: String) = decode[AccountDetail](body)
