package io.leisuremeta.chain.lib
package codec.byte

import scala.deriving.Mirror
import scala.reflect.{ClassTag, classTag}

import scodec.bits.ByteVector

import datatype.UInt256

trait ByteEncoder[A]:
  def encode(a: A): ByteVector

  def contramap[B](f: B => A): ByteEncoder[B] = b => encode(f(b))

object ByteEncoder:
  def apply[A: ByteEncoder]: ByteEncoder[A] = summon

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

  given [A: UInt256.Ops]: ByteEncoder[UInt256.Refined[A]] = _.toBytes

  object ops:
    extension [A](a: A)
      def toBytes(implicit be: ByteEncoder[A]): ByteVector = be.encode(a)
