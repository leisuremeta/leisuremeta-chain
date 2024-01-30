package io.leisuremeta.chain
package api.model
package token

import lib.crypto.Hash
import lib.datatype.{BigNat, Utf8}

final case class NftState(
    tokenId: TokenId,
    tokenDefinitionId: TokenDefinitionId,
    rarity: Rarity,
    weight: BigNat,
    currentOwner: Account,
    memo: Option[Utf8],
    lastUpdateTx: Hash.Value[TransactionWithResult],
    previousState: Option[Hash.Value[TransactionWithResult]],
)
