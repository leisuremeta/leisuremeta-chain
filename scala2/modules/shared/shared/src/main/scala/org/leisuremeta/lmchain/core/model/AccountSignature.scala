package org.leisuremeta.lmchain.core
package model

import cats.implicits._

import codec.byte.{ByteDecoder, ByteEncoder}
import crypto.Signature
import datatype.BigNat
import ByteEncoder.ops._

sealed trait AccountSignature {
  def sig: Signature
}

object AccountSignature {

  final case class NamedSignature(
      name: Account.Name,
      sig: Signature,
  ) extends AccountSignature

  final case class UnnamedSignature(
      sig: Signature
  ) extends AccountSignature

  implicit val encoder: ByteEncoder[AccountSignature] = {
    case NamedSignature(name, sig) =>
      val nameSizeBytes = BigNat.unsafeFromLong(name.bytes.size).toBytes
      val nameBytes     = name.bytes
      nameSizeBytes ++ nameBytes ++ sig.toBytes
    case UnnamedSignature(sig) =>
      BigNat.Zero.toBytes ++ sig.toBytes
  }

  implicit val decoder: ByteDecoder[AccountSignature] =
    ByteDecoder[BigNat].flatMap {
      case size if size.value === BigInt(0) =>
        ByteDecoder[Signature].map(UnnamedSignature(_))
      case size =>
        for {
          name <- ByteDecoder.fromFixedSizeBytes(size.value.toLong)(
            Account.Name.unsafeFromBytes
          )
          sig <- ByteDecoder[Signature]
        } yield NamedSignature(name, sig)
    }
}
