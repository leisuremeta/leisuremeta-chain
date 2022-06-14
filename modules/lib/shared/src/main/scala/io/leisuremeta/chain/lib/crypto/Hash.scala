package io.leisuremeta.chain.lib
package crypto

import cats.Eq
import cats.Contravariant

import io.circe.{Decoder, Encoder, KeyEncoder, KeyDecoder}
import scodec.bits.ByteVector

import codec.byte.{ByteDecoder, ByteEncoder}
import datatype.{UInt256, UInt256Bytes}

trait Hash[A]:
  def apply(a: A): Hash.Value[A]
  def contramap[B](f: B => A): Hash[B] = (b: B) =>
    Hash.Value[B](apply(f(b)).toUInt256Bytes)

object Hash:
  def apply[A: Hash]: Hash[A] = summon

  opaque type Value[A] = UInt256Bytes
  object Value:
    def apply[A](uint256: UInt256Bytes): Value[A] = uint256

    given circeValueDecoder[A]: Decoder[Value[A]] =
      UInt256.uint256bytesCirceDecoder.map(Value[A](_))

    given circeValueEncoder[A]: Encoder[Value[A]] =
      UInt256.uint256bytesCirceEncoder.contramap[Value[A]](_.toUInt256Bytes)

    given circeKeyDecoder[A]: KeyDecoder[Value[A]] = (str) =>
      for
        bytes <- ByteVector.fromHex(str)
        uint256 <- UInt256.from(bytes).toOption
      yield Value[A](uint256)

    given circeKeyEncoder[A]: KeyEncoder[Value[A]] =
      KeyEncoder.encodeKeyString.contramap[Value[A]](_.toUInt256Bytes.toBytes.toHex)

    given byteValueDecoder[A]: ByteDecoder[Value[A]] =
      UInt256.uint256bytesByteDecoder.map(Value[A](_))

    given byteValueEncoder[A]: ByteEncoder[Value[A]] =
      UInt256.uint256bytesByteEncoder.contramap[Value[A]](_.toUInt256Bytes)

    given eqValue[A]: Eq[Value[A]] = Eq.fromUniversalEquals


  extension [A](value: Value[A]) def toUInt256Bytes: UInt256Bytes = value

  object ops:
    extension [A](a: A) def toHash(using h: Hash[A]): Value[A] = h(a)

  given contravariant: Contravariant[Hash] = new Contravariant[Hash]:
    override def contramap[A, B](fa: Hash[A])(f: B => A): Hash[B] =
      fa.contramap(f)

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  def build[A: ByteEncoder]: Hash[A] = (a: A) =>
    val bytes = ByteEncoder[A].encode(a)
    val h     = ByteVector.view(CryptoOps.keccak256(bytes.toArray))
    Value[A](UInt256.from(h).toOption.get)
