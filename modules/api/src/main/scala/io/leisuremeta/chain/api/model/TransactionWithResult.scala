package io.leisuremeta.chain
package api.model

import lib.crypto.Hash

final case class TransactionWithResult(
    signedTx: Signed.Tx,
    result: TransactionResult,
)

object TransactionWithResult:
  given Hash[TransactionWithResult] =
    Hash[Transaction].contramap(_.signedTx.value)
