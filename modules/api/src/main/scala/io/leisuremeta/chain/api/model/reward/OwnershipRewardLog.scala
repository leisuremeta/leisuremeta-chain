package io.leisuremeta.chain
package api.model
package reward

import lib.crypto.Hash

final case class OwnershipRewardLog(
    activitySnapshot: OwnershipSnapshot,
    txHash: Hash.Value[TransactionWithResult],
)
