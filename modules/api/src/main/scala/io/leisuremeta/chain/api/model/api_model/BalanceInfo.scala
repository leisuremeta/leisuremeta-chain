package io.leisuremeta.chain
package api.model
package api_model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*

import lib.crypto.Hash
import lib.datatype.BigNat

final case class BalanceInfo(
    totalAmount: BigNat,
    unused: Map[Hash.Value[TransactionWithResult], TransactionWithResult],
)

//object BalanceInfo:
//  given circeBalanceInfoCodec: Encoder[BalanceInfo] =
//    summon[Encoder[BigNat]]
//    summon[io.circe.KeyEncoder[Hash.Value[TransactionWithResult]]]
//    summon[Encoder[TransactionResult]]
//    deriveEncoder
