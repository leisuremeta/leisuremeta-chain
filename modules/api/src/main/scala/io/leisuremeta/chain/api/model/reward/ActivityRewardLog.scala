package io.leisuremeta.chain
package api.model
package reward

import lib.crypto.Hash

final case class ActivityRewardLog(
    activitySnapshot: ActivitySnapshot,
    txHash: Hash.Value[TransactionWithResult],
)
