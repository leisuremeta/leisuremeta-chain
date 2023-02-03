package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import io.circe.Encoder.*

case class AccountDetail(
    address: String,
    balance: Double,
    value: Double,
    txHistory: List[AccountDetailTxHist],
)

case class AccountDetailTxHist(
    hash: String,
    blockNumber: Int,
    createdAt: Int,
    txType: String,
    tokenType: String,
    signer: String,
    value: String,
)
case class AccountDetailTxHistList(
    totalCount: Int,
    totalPages: Int,
    payload: List[AccountDetailTxHist],
)

object AccountDetailParser:
  given accountDetailDecoder: Decoder[AccountDetail] =
    deriveDecoder[AccountDetail]
  given accountDetailTxHistDecoder: Decoder[AccountDetailTxHist] =
    deriveDecoder[AccountDetailTxHist]
  def decodeParser(body: String) = decode[AccountDetail](body)

  given txHistListEncoder: Encoder[AccountDetailTxHistList] =
    deriveEncoder[AccountDetailTxHistList]
  given txHistEncoder: Encoder[AccountDetailTxHist] =
    deriveEncoder[AccountDetailTxHist]
  def txEncodeParser(list: List[AccountDetailTxHist]) =
    AccountDetailTxHistList(20, 1, list).asJson.spaces2
