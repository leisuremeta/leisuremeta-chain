package io.leisuremeta.chain
package api.model
package api_model

import io.circe.generic.semiauto.*

import lib.crypto.Hash
import lib.datatype.BigNat

final case class BalanceInfo(
    totalAmount: BigNat,
    unused: Map[Hash.Value[TransactionWithResult], TransactionWithResult],
)
