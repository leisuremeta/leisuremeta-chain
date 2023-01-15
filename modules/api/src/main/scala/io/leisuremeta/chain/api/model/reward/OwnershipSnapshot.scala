package io.leisuremeta.chain
package api.model
package reward

import lib.datatype.BigNat

final case class OwnershipSnapshot(
    account: Account,
    score: BigNat,
)
