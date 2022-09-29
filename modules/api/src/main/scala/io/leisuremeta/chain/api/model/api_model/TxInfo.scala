package io.leisuremeta.chain
package api.model
package api_model

import java.time.Instant

final case class TxInfo(
    txHash: Signed.TxHash,
    createdAt: Instant,
    account: Account,
    `type`: String,
)
