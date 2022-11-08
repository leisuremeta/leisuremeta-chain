package io.leisuremeta.chain.lib
package datatype

import cats.implicits.given

import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import io.circe.{
  Decoder as CirceDecoder,
  Encoder as CirceEncoder,
  KeyDecoder,
  KeyEncoder,
}
import io.circe.generic.auto.given
import io.circe.refined.*
import scodec.bits.ByteVector

import codec.byte.{ByteDecoder, ByteEncoder, DecodeResult}
import failure.DecodingFailure

opaque type BigNat = BigInt Refined NonNegative

object BigNat:
  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  val Zero: BigNat = refineV[NonNegative](BigInt(0)).toOption.get
  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  val One: BigNat = refineV[NonNegative](BigInt(1)).toOption.get

  def fromBigInt(n: BigInt): Either[String, BigNat] = refineV[NonNegative](n)

  extension (bignat: BigNat)
    @annotation.targetName("plus")
    def +(that: BigNat): BigNat = BigNat.add(bignat, that)

    @annotation.targetName("times")
    def *(that: BigNat): BigNat = BigNat.multiply(bignat, that)

    @annotation.targetName("diviedBy")
    def /(that: BigNat): BigNat = BigNat.divide(bignat, that)

    def toBigInt: BigInt = bignat.value

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def unsafeFromBigInt(n: BigInt): BigNat = fromBigInt(n) match
    case Right(nat) => nat
    case Left(e)    => throw new Exception(e)

  def unsafeFromLong(long: Long): BigNat = unsafeFromBigInt(BigInt(long))

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def add(x: BigNat, y: BigNat): BigNat =
    refineV[NonNegative](x.value + y.value) match
      case Right(nat) => nat
      case Left(e)    => throw new Exception(e)

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def multiply(x: BigNat, y: BigNat): BigNat =
    refineV[NonNegative](x.value * y.value) match
      case Right(nat) => nat
      case Left(e)    => throw new Exception(e)

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def divide(x: BigNat, y: BigNat): BigNat =
    refineV[NonNegative](x.value / y.value) match
      case Right(nat) => nat
      case Left(e)    => throw new Exception(e)

  given bignatByteDecoder: ByteDecoder[BigNat] = ByteDecoder.bignatByteDecoder

  given bignatByteEncoder: ByteEncoder[BigNat] = ByteEncoder.bignatByteEncoder

  given bignatCirceDecoder: CirceDecoder[BigNat] =
    refinedDecoder[BigInt, NonNegative, Refined]

  given bignatCirceEncoder: CirceEncoder[BigNat] =
    refinedEncoder[BigInt, NonNegative, Refined]
