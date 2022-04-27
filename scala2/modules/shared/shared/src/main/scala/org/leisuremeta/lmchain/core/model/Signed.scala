package org.leisuremeta.lmchain.core
package model

import crypto.Hash
import io.circe.{Decoder, Encoder}

final case class Signed[A](sig: AccountSignature, value: A)

object Signed {
  type Tx = Signed[Transaction]

  type TxHash = Hash.Value[Tx]

  implicit def signedHash[A: Hash]: Hash[Signed[A]] = Hash[A].contramap(_.value)

  implicit val txhashDecoder: Decoder[TxHash] = Hash.circeValueDecoder[Tx]
  implicit val txhashEncoder: Encoder[TxHash] = Hash.circeValueEncoder[Tx]
}
