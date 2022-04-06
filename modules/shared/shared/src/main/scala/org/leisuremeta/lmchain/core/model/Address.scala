package org.leisuremeta.lmchain.core
package model

import cats.Eq
import cats.implicits._

import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import scodec.bits.ByteVector

import codec.byte.{ByteDecoder, ByteEncoder}
import crypto.{Hash, PublicKey}
import failure.EncodingFailure

final case class Address private (bytes: ByteVector) extends AnyVal {
  override def toString: String = bytes.toHex
}

object Address {

  def apply(bytes: ByteVector): Either[EncodingFailure, Address] = Either.cond(
    bytes.size === 20L,
    new Address(bytes),
    EncodingFailure(s"Byte size is not 20: $bytes"),
  )

  def fromPublicKeyHash(hash: Hash.Value[PublicKey]): Address = new Address(
    hash.toBytes takeRight 20
  )

  def fromHex(hexString: String): Either[EncodingFailure, Address] = for {
    bytes <- ByteVector
      .fromHexDescriptive(hexString)
      .left
      .map(EncodingFailure(_))
    address <- Address(bytes)
  } yield address

  implicit val AddressEqInstance: Eq[Address] = Eq.fromUniversalEquals

  implicit val addressEncoder: ByteEncoder[Address] = _.bytes

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  implicit val addressDecoder: ByteDecoder[Address] =
    ByteDecoder.fromFixedSizeBytes(20) { Address(_).toOption.get }

  implicit val addressCirceKeyDecoder: KeyDecoder[Address] =
    KeyDecoder.instance(Address.fromHex(_).toOption)
  implicit val addressCirceKeyEncoder: KeyEncoder[Address] =
    KeyEncoder.encodeKeyString.contramap(_.toString)

  implicit val addressCirceDecoder: Decoder[Address] =
    Decoder.decodeString.emap { (s: String) =>
      val (f, b) = s `splitAt` 2
      for {
        _       <- Either.cond(f === "0x", (), s"Address string not starting 0x: $f")
        address <- Address.fromHex(b).left.map(_.msg)
      } yield address
    }
  implicit val addressCirceEncoder: Encoder[Address] =
    Encoder.encodeString.contramap(address => s"0x${address.toString}")
}
