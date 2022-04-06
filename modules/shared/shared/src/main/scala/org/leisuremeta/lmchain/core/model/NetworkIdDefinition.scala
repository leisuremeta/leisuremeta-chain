package org.leisuremeta.lmchain.core
package model

import io.circe.refined._
import io.circe.{Decoder, Encoder}
import shapeless.tag
import shapeless.tag.@@

import codec.byte.{ByteDecoder, ByteEncoder}
import codec.circe._
import datatype.BigNat

trait NetworkIdDefinition {
  trait NetworkIdTag
  type NetworkId = BigNat @@ NetworkIdTag

  object NetworkId {
    def apply(n: BigNat): NetworkId = tag[NetworkIdTag][BigNat](n)
  }

  implicit val networkIdDecoder: ByteDecoder[NetworkId] =
    ByteDecoder[BigNat].map(NetworkId(_))
  implicit val networkIdEncoder: ByteEncoder[NetworkId] =
    ByteEncoder[BigNat].contramap(_.asInstanceOf[BigNat])

  implicit val circeDecoder: Decoder[NetworkId] =
    taggedDecoder[BigNat, NetworkIdTag]

  implicit val circeEncoder: Encoder[NetworkId] =
    taggedEncoder[BigNat, NetworkIdTag]
}
