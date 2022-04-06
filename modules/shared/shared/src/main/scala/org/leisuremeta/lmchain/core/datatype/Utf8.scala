package org.leisuremeta.lmchain.core.datatype

import scodec.bits.ByteVector

final case class Utf8 private (
    str: String,
    bytes: ByteVector,
)

object Utf8 {
  def from(str: String): Either[String, Utf8] =
    ByteVector.encodeUtf8(str) match {
      case Left(cce)    => Left(cce.getMessage())
      case Right(bytes) => Right(Utf8(str, bytes))
    }

  def decode(bytes: ByteVector): Either[String, Utf8] = bytes.decodeUtf8 match {
    case Left(e)    => Left(e.getMessage())
    case Right(str) => Right(Utf8(str, bytes))
  }

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def unsafeFrom(str: String): Utf8 = ByteVector.encodeUtf8(str) match {
    case Left(cce)    => throw cce
    case Right(bytes) => Utf8(str, bytes)
  }

  def unsafeApply(str: String, bytes: ByteVector): Utf8 = Utf8(str, bytes)

}
