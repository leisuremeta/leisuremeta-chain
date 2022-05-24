package io.leisuremeta.chain
package api.model

import lib.crypto.Hash
import io.circe.{Decoder, Encoder}

final case class Signed[A](sig: AccountSignature, value: A)

object Signed:
  type Tx = Signed[Transaction]

  type TxHash = Hash.Value[Tx]

  given signedHash[A: Hash]: Hash[Signed[A]] = Hash[A].contramap(_.value)

  given txhashDecoder: Decoder[TxHash] = Hash.Value.circeValueDecoder[Tx]
  given txhashEncoder: Encoder[TxHash] = Hash.Value.circeValueEncoder[Tx]

  import io.circe.generic.semiauto.*

  given signedDecoder[A: Decoder]: Decoder[Signed[A]] = deriveDecoder
  given signedEncoder[A: Encoder]: Encoder[Signed[A]] = deriveEncoder
