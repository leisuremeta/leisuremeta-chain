package io.leisuremeta.chain
package api.model
package reward

import java.time.Instant

import lib.datatype.BigNat
import token.TokenDefinitionId

final case class ActivitySnapshot(
    account: Account,
    from: Instant,
    to: Instant,
    point: BigInt,
    definitionId: TokenDefinitionId,
    amount: BigNat,
    backlogs: Set[Signed.TxHash],
)
