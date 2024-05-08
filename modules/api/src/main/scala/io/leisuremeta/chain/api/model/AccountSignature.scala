package io.leisuremeta.chain
package api.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*

import lib.crypto.Signature

final case class AccountSignature(
    sig: Signature,
    account: Account,
)

object AccountSignature:
  given Decoder[AccountSignature] = deriveDecoder
  given Encoder[AccountSignature] = deriveEncoder
