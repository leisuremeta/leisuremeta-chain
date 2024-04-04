package io.leisuremeta.chain.lib
package datatype

import scala.math.Ordering

import cats.Eq
import cats.implicits.given

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import io.circe.{
  Decoder as CirceDecoder,
  Encoder as CirceEncoder,
}
import io.circe.refined.*

import codec.byte.{ByteDecoder, ByteEncoder}

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

    def floorAt(e: Int): BigNat =
      val n = BigInt(10).pow(e)
      unsafeFromBigInt(bignat.value / n * n)

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

  def tryToSubtract(x: BigNat, y: BigNat): Either[String, BigNat] =
    fromBigInt(x.toBigInt - y.toBigInt)

  def max(x: BigNat, y: BigNat): BigNat =
    if x.toBigInt >= y.toBigInt then x else y

  def min(x: BigNat, y: BigNat): BigNat =
    if x.toBigInt <= y.toBigInt then x else y

  given bignatByteDecoder: ByteDecoder[BigNat] = ByteDecoder.bignatByteDecoder

  given bignatByteEncoder: ByteEncoder[BigNat] = ByteEncoder.bignatByteEncoder

  given bignatCirceDecoder: CirceDecoder[BigNat] =
    refinedDecoder[BigInt, NonNegative, Refined]

  given bignatCirceEncoder: CirceEncoder[BigNat] =
    refinedEncoder[BigInt, NonNegative, Refined]

  given bignatOrdering: Ordering[BigNat] = Ordering.by(_.toBigInt)

  given bignatEq: Eq[BigNat] = Eq.by(_.toBigInt)
