package io.leisuremeta.chain
package api.model
package token

import lib.datatype.BigNat

final case class NftState(
    tokenId: TokenId,
    tokenDefinitionId: TokenDefinitionId,
    rarity: Rarity,
    weight: BigNat,
    currentOwner: Account,
)
