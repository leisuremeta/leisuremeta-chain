package io.leisuremeta.chain
package api.model
package reward

import lib.crypto.Hash
import lib.datatype.Utf8

final case class ActivityLog(
  point: BigInt,
  description: Utf8,
  txHash: Hash.Value[TransactionWithResult],
)
