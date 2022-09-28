package io.leisuremeta.chain
package api.model.reward

import cats.Monoid

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

  given monoid: Monoid[DaoActivity] = Monoid.instance(
    emptyValue = DaoActivity(BigNat.Zero, BigNat.Zero, BigNat.Zero, BigNat.Zero),
    cmb = (a1, a2) => DaoActivity(
      like = BigNat.add(a1.like, a2.like),
      comment = BigNat.add(a1.comment, a2.comment),
      share = BigNat.add(a1.share, a2.share),
      report = BigNat.add(a1.report, a2.report),
    )
  )
