package io.leisuremeta.chain.lib
package codec.byte

import java.time.Instant

import scala.compiletime.{erasedValue, summonInline}
import scala.deriving.Mirror
import scala.reflect.{ClassTag, classTag}

import cats.syntax.eq.catsSyntaxEq
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import scodec.bits.ByteVector
import shapeless3.deriving.*

import datatype.UInt256

trait ByteEncoder[A]:
  def encode(a: A): ByteVector

  def contramap[B](f: B => A): ByteEncoder[B] = b => encode(f(b))

object ByteEncoder:
  def apply[A: ByteEncoder]: ByteEncoder[A] = summon

  object ops:
    extension [A](a: A)
      def toBytes(implicit be: ByteEncoder[A]): ByteVector = be.encode(a)

  type BigNat = BigInt Refined NonNegative

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def unsafeFromBigInt(n: BigInt): BigNat = refineV[NonNegative](n) match
    case Right(nat) => nat
    case Left(e)    => throw new Exception(e)

  given ByteEncoder[EmptyTuple] = _ => ByteVector.empty

  given [H, T <: Tuple](using
      beh: ByteEncoder[H],
      bet: ByteEncoder[T],
  ): ByteEncoder[H *: T] = { case h *: t =>
    beh.encode(h) ++ bet.encode(t)
  }

  given genericEncoder[P <: Product](using
      m: Mirror.ProductOf[P],
      beb: ByteEncoder[m.MirroredElemTypes],
  ): ByteEncoder[P] = beb contramap Tuple.fromProductTyped

  private inline def summonAll[T <: Tuple]: List[ByteEncoder[_]] = inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts)  => summonInline[ByteEncoder[t]] :: summonAll[ts]

  inline given sumEncoder[S](using s: Mirror.SumOf[S]): ByteEncoder[S] =
    lazy val elemInstances = summonAll[s.MirroredElemTypes]

    (sv: S) =>
      val ordinal = s.ordinal(sv)
      bignatByteEncoder.encode(unsafeFromBigInt(ordinal)) ++ elemInstances(
        ordinal,
      ).asInstanceOf[ByteEncoder[S]].encode(sv)


  given unitByteEncoder: ByteEncoder[Unit] = _ => ByteVector.empty

  given [A: UInt256.Ops]: ByteEncoder[UInt256.Refined[A]] = _.toBytes

  given instantEncoder: ByteEncoder[Instant] =
    ByteVector fromLong _.toEpochMilli

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

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  given bigintByteEncoder: ByteEncoder[BigInt] = ByteEncoder[BigNat].contramap{
    case n if n >= 0 => (n * 2).asInstanceOf[BigNat]
    case n => (n * (-2) + 1).asInstanceOf[BigNat]
  }

  given listByteEncoder[A: ByteEncoder]: ByteEncoder[List[A]] =
    (list: List[A]) =>
      list.foldLeft(bignatByteEncoder.encode(unsafeFromBigInt(list.size))) {
        case (acc, a) => acc ++ ByteEncoder[A].encode(a)
      }

  given mapByteEncoder[K: ByteEncoder, V: ByteEncoder]: ByteEncoder[Map[K, V]] =
    listByteEncoder[(K, V)].contramap(_.toList)

  given optionByteEncoder[A: ByteEncoder]: ByteEncoder[Option[A]] =
    listByteEncoder.contramap(_.toList)

  given setByteEncoder[A: ByteEncoder]: ByteEncoder[Set[A]] = (set: Set[A]) =>
    set
      .map(ByteEncoder[A].encode)
      .toList
      .sorted
      .foldLeft {
        bignatByteEncoder.encode(unsafeFromBigInt(set.size))
      }(_ ++ _)
