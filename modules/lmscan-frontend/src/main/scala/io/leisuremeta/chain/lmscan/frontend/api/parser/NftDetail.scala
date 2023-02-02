package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*

case class NftDetail(
    nftFile: NftFile,
    activities: List[NftActivities],
)

case class NftFile(
    tokenId: String,
    tokenDefId: String,
    collectionName: String,
    nftName: String,
    nftUri: String,
    creatorDescription: String,
    dataUrl: String,
    rarity: String,
    creator: String,
    eventTime: Int,
    createdAt: Int,
    owner: String,
)

case class NftActivities(
    txHash: String,
    action: String,
    fromAddr: String,
    toAddr: String,
    createdAt: Int,
)

object NftDetailParser:
  implicit val nftFileDecoder: Decoder[NftFile] = deriveDecoder[NftFile]
  implicit val nftActivitiesDecoder: Decoder[NftActivities] =
    deriveDecoder[NftActivities]
  implicit val txDetailDecoder: Decoder[NftDetail] = deriveDecoder[NftDetail]
  def decodeParser(body: String)                   = decode[NftDetail](body)
