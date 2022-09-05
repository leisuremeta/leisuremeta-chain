package io.leisuremeta.chain
package api.model.reward

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*

import lib.datatype.BigNat

final case class DaoActivity(
    like: BigNat,
    comment: BigNat,
    share: BigNat,
    report: BigNat,
)

object DaoActivity:
  given circeDecoder: Decoder[DaoActivity] = deriveDecoder
  given circeEncoder: Encoder[DaoActivity] = deriveEncoder
