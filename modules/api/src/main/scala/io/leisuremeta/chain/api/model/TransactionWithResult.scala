package io.leisuremeta.chain.api.model

final case class TransactionWithResult(
    signedTx: Signed.Tx,
    result: TransactionResult,
)
