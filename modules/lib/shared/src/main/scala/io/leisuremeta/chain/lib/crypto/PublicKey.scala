package io.leisuremeta.chain.lib
package crypto

import cats.implicits.given

import scodec.bits.ByteVector

import codec.byte.{ByteDecoder, ByteEncoder}
import datatype.{UInt256BigInt, UInt256}
import failure.UInt256RefineFailure
import io.leisuremeta.chain.lib.failure.DecodingFailure

final case class PublicKey(x: UInt256BigInt, y: UInt256BigInt):
  def toBytes: ByteVector = x.toBytes ++ y.toBytes
  def toBigInt: BigInt    = BigInt(1, toBytes.toArray)

  override def toString: String = s"PublicKey($toBytes)"

object PublicKey:
  @SuppressWarnings(Array("org.wartremover.warts.Nothing"))
  def fromByteArray(
      array: Array[Byte],
  ): Either[UInt256RefineFailure, PublicKey] =
    if array.length =!= 64 then
      Left(
        UInt256RefineFailure(s"Public key array size are not 64: $array"),
      )
    else
      val (xArr, yArr) = array splitAt 32
      for
        x <- UInt256.from(BigInt(1, xArr))
        y <- UInt256.from(BigInt(1, yArr))
      yield PublicKey(x, y)

  val pubkeyByteEncoder: ByteEncoder[PublicKey] = _.toBytes
  given pubkeyByteDecoder: ByteDecoder[PublicKey] =
    ByteDecoder.fromFixedSizeBytes(64)(identity).emap { bytes =>
      fromByteArray(bytes.toArray).left.map(e => DecodingFailure(e.msg))
    }

  given Hash[PublicKey] = Hash.build
