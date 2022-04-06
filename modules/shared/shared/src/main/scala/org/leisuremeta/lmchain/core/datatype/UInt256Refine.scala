package org.leisuremeta.lmchain.core.datatype

import scala.math.Ordering
import cats.Eq
import cats.implicits._
import scodec.bits.ByteVector

trait UInt256Refine[A]

object UInt256Refine {

  type UInt256Refined[A] = A with UInt256Refine[A]
  type UInt256Bytes      = UInt256Refined[ByteVector]
  type UInt256BigInt     = UInt256Refined[BigInt]

  def from[A: UInt256RefineOps](a: A): Either[String, UInt256Refined[A]] =
    UInt256RefineOps[A].from(a)

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  val EmptyBytes: UInt256Bytes =
    UInt256Refine.from(ByteVector.low(32)).toOption.get

  implicit class UInt256RefinedSyntax[A](val uint256: UInt256Refined[A])
      extends AnyVal {
    def toBytes(implicit ops: UInt256RefineOps[A]): ByteVector =
      ops.toBytes(uint256)
    def toBigInt(implicit ops: UInt256RefineOps[A]): BigInt =
      ops.toBigInt(uint256)
  }

  trait UInt256RefineOps[A] {
    def from(value: A): Either[String, UInt256Refined[A]]
    def toBytes(value: UInt256Refined[A]): ByteVector
    def toBigInt(value: UInt256Refined[A]): BigInt
  }
  object UInt256RefineOps {
    def apply[A](implicit ops: UInt256RefineOps[A]): UInt256RefineOps[A] = ops

    implicit val refineBytes: UInt256RefineOps[ByteVector] =
      new UInt256RefineOps[ByteVector] {
        @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
        def from(value: ByteVector): Either[String, UInt256Bytes] = Either.cond(
          value.size === 32,
          value.asInstanceOf[UInt256Bytes],
          s"Incorrect sized bytes to be UInt256: $value",
        )
        def toBytes(value: UInt256Bytes): ByteVector = value
        def toBigInt(value: UInt256Bytes): BigInt    = BigInt(1, value.toArray)
      }

    implicit val refineBigInt: UInt256RefineOps[BigInt] =
      new UInt256RefineOps[BigInt] {
        @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
        def from(value: BigInt): Either[String, UInt256BigInt] = Either.cond(
          value >= 0 && value.bitLength <= 256,
          value.asInstanceOf[UInt256BigInt],
          s"Bigint out of range to be UInt256: $value",
        )
        def toBytes(value: UInt256BigInt): ByteVector =
          ByteVector.view(value.toByteArray).takeRight(32L).padLeft(32L)

        def toBigInt(value: UInt256BigInt): BigInt = value
      }
  }

  implicit def UInt256EqInstance[A]: Eq[UInt256Refined[A]] =
    Eq.fromUniversalEquals

  implicit def uint256Ordering[A: UInt256RefineOps]
      : Ordering[UInt256Refined[A]] = Ordering.by(_.toBigInt)
}
