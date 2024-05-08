package io.leisuremeta.chain
package api.model
package token

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*

import lib.datatype.{BigNat, UInt256Bytes, Utf8}

final case class NftInfo(
    minter: Account,
    rarity: Map[Rarity, BigNat],
    dataUrl: Utf8,
    contentHash: UInt256Bytes,
)
object NftInfo:
  given Decoder[NftInfo] = deriveDecoder
  given Encoder[NftInfo] = deriveEncoder
