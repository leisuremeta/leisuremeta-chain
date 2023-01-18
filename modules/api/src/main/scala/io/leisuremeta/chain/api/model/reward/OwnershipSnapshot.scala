package io.leisuremeta.chain
package api.model
package reward

import java.time.Instant

import lib.datatype.BigNat
import token.TokenDefinitionId

final case class OwnershipSnapshot(
    account: Account,
    timestamp: Instant,
    point: BigNat,
    definitionId: TokenDefinitionId,
    amount: BigNat,
)
