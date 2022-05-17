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
    def toBigInt: BigInt = bignat.value

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def unsafeFromBigInt(n: BigInt): BigNat = fromBigInt(n) match
    case Right(nat) => nat
    case Left(e)    => throw new Exception(e)

  def unsafeFromLong(long: Long): BigNat = unsafeFromBigInt(BigInt(long))

  given bignatByteDecoder: ByteDecoder[BigNat] = bytes =>
    Either.cond(bytes.nonEmpty, bytes, DecodingFailure("Empty bytes")).flatMap {
      nonEmptyBytes =>
        val head: Int        = nonEmptyBytes.head & 0xff
        val tail: ByteVector = nonEmptyBytes.tail
        if head <= 0x80 then
          Right[DecodingFailure, DecodeResult[BigNat]](
            DecodeResult(unsafeFromBigInt(BigInt(head)), tail),
          )
        else if head <= 0xf8 then
          val size = head - 0x80
          if tail.size < size then
            Left[DecodingFailure, DecodeResult[BigNat]](
              DecodingFailure(
                s"required byte size $size, but $tail",
              ),
            )
          else
            val (front, back) = tail.splitAt(size.toLong)
            Right[DecodingFailure, DecodeResult[BigNat]](
              DecodeResult(unsafeFromBigInt(BigInt(1, front.toArray)), back),
            )
        else
          val sizeOfNumber = head - 0xf8 + 1
          if tail.size < sizeOfNumber then
            Left[DecodingFailure, DecodeResult[BigNat]](
              DecodingFailure(
                s"required byte size $sizeOfNumber, but $tail",
              ),
            )
          else
            val (sizeBytes, data) = tail.splitAt(sizeOfNumber.toLong)
            val size              = BigInt(1, sizeBytes.toArray).toLong

            if data.size < size then
              Left[DecodingFailure, DecodeResult[BigNat]](
                DecodingFailure(
                  s"required byte size $size, but $data",
                ),
              )
            else
              val (front, back) = data.splitAt(size)
              Right[DecodingFailure, DecodeResult[BigNat]](
                DecodeResult(unsafeFromBigInt(BigInt(1, front.toArray)), back),
              )
    }

  given bignatByteEncoder: ByteEncoder[BigNat] = bignat =>
    val bytes = ByteVector.view(bignat.toByteArray).dropWhile(_ === 0x00.toByte)
    if bytes.isEmpty then ByteVector(0x00.toByte)
    else if bignat <= 0x80 then bytes
    else
      val size = bytes.size
      if size < (0xf8 - 0x80) + 1 then
        ByteVector.fromByte((size + 0x80).toByte) ++ bytes
      else
        val sizeBytes = ByteVector.fromLong(size).dropWhile(_ === 0x00.toByte)
        ByteVector.fromByte(
          (sizeBytes.size + 0xf8 - 1).toByte,
        ) ++ sizeBytes ++ bytes

  given bignatCirceDecoder: CirceDecoder[BigNat] = refinedDecoder[BigInt, NonNegative, Refined]

  given bignatCirceEncoder: CirceEncoder[BigNat] = refinedEncoder[BigInt, NonNegative, Refined]
