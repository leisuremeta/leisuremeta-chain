package io.leisuremeta.chain
package api.model.reward

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*

import lib.datatype.Utf8

final case class DaoActivity(
    point: BigInt,
    description: Utf8,
)

object DaoActivity:
  given circeDecoder: Decoder[DaoActivity] = deriveDecoder
  given circeEncoder: Encoder[DaoActivity] = deriveEncoder
