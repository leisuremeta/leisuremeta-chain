package io.leisuremeta.chain
package api.model
package token

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*

import lib.datatype.{BigNat, UInt256Bytes, Utf8}

final case class NftInfoWithPrecision(
    minter: Account,
    rarity: Map[Rarity, BigNat],
    precision: BigNat,
    dataUrl: Utf8,
    contentHash: UInt256Bytes,
)

object NftInfoWithPrecision:
  def fromNftInfo(nftInfo: NftInfo): NftInfoWithPrecision =
    NftInfoWithPrecision(
      nftInfo.minter,
      nftInfo.rarity,
      BigNat.Zero,
      nftInfo.dataUrl,
      nftInfo.contentHash,
    )

  given Decoder[NftInfoWithPrecision] = deriveDecoder
  given Encoder[NftInfoWithPrecision] = deriveEncoder
