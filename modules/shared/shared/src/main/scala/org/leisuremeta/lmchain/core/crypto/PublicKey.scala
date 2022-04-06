package org.leisuremeta.lmchain.core
package crypto

import cats.implicits._

import scodec.bits.ByteVector

import datatype.{UInt256BigInt, UInt256Refine}

final case class PublicKey(x: UInt256BigInt, y: UInt256BigInt) {
  def toBytes: ByteVector = x.toBytes ++ y.toBytes
  def toBigInt: BigInt    = BigInt(1, toBytes.toArray)

  override def toString: String = s"PublicKey($toBytes)"
}

object PublicKey {
  def fromByteArray(array: Array[Byte]): Either[String, PublicKey] = {
    if (array.size =!= 64) Left(s"Array size are not 64: $array")
    else {
      val (xArr, yArr) = array splitAt 32
      for {
        x <- UInt256Refine from BigInt(1, xArr)
        y <- UInt256Refine from BigInt(1, yArr)
      } yield PublicKey(x, y)
    }
  }
}
