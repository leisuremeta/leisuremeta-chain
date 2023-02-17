package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.parser.*

case class AccountDetail(
    address: Option[String] = None,
    balance: Option[Double] = None,
    value: Option[Double] = None,
    txHistory: Option[List[Tx]] = None,
)

object AccountDetailParser:
  given accountDetailDecoder: Decoder[AccountDetail] = deriveDecoder[AccountDetail]
  given txDecoder: Decoder[Tx]   = deriveDecoder[Tx]
  def decodeParser(body: String) = decode[AccountDetail](body)
