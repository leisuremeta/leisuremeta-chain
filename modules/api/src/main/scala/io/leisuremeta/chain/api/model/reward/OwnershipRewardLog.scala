package io.leisuremeta.chain
package api.model
package reward

import lib.crypto.Hash

final case class OwnershipRewardLog(
    ownershipSnapshot: OwnershipSnapshot,
    txHash: Hash.Value[TransactionWithResult],
)
