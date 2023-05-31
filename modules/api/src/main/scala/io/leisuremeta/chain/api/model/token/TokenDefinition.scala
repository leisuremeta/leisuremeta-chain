package io.leisuremeta.chain
package api.model
package token

import lib.datatype.{BigNat, UInt256Bytes, Utf8}

final case class TokenDefinition(
    id: TokenDefinitionId,
    name: Utf8,
    symbol: Option[Utf8],
    adminGroup: Option[GroupId],
    totalAmount: BigNat,
    nftInfo: Option[NftInfoWithPrecision],
)
