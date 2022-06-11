package io.leisuremeta.chain.api.model
package token

final case class NftState(
    tokenId: TokenId,
    tokenDefinitionId: TokenDefinitionId,
    currentOwner: Account,
)
