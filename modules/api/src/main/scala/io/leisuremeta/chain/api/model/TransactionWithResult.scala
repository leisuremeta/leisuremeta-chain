package io.leisuremeta.chain
package api.model

import lib.crypto.Hash

final case class TransactionWithResult(
    signedTx: Signed.Tx,
    result: Option[TransactionResult],
)

object TransactionWithResult:
  given Hash[TransactionWithResult] =
    Hash[Transaction].contramap(_.signedTx.value)

  object ops:
    extension [A](txHash: Hash.Value[A])
      def toResultHashValue: Hash.Value[TransactionWithResult] =
        Hash.Value[TransactionWithResult](txHash.toUInt256Bytes)
