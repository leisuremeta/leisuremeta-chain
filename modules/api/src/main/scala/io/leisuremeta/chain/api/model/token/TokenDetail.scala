package io.leisuremeta.chain
package api.model.token

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*

import lib.datatype.BigNat

sealed trait TokenDetail

object TokenDetail:
  final case class FungibleDetail(amount: BigNat) extends TokenDetail
  final case class NftDetail(tokenId: TokenId) extends TokenDetail

  given tokenDetailCirceEncoder: Encoder[TokenDetail] = deriveEncoder
  given tokenDetailCirceDecoder: Decoder[TokenDetail] = deriveDecoder
