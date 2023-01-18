package io.leisuremeta.chain
package api.model.reward

import cats.Monoid

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*

import lib.datatype.{BigNat, Utf8}

final case class DaoActivity(
    weight: BigInt,
    count: BigNat,
)

object DaoActivity:
  given circeDecoder: Decoder[DaoActivity] = deriveDecoder
  given circeEncoder: Encoder[DaoActivity] = deriveEncoder
