package io.leisuremeta.chain.lib.util.iron

import scala.compiletime.{summonInline}

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.collection.*

import scodec.bits.{BitVector, ByteVector}

@SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
class LengthBitVector[C, Impl <: Constraint[Long, C]](using Impl)
    extends Constraint[BitVector, Length[C]]:
  override inline def test(value: BitVector): Boolean =
    summonInline[Impl].test(value.size)
  override inline def message: String =
    s"Length: (${summonInline[Impl].message})"

@SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
inline given [C, Impl <: Constraint[Long, C]](using
    inline impl: Impl,
): LengthBitVector[C, Impl] = new LengthBitVector[C, Impl]

@SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
class LengthByteVector[C, Impl <: Constraint[Long, C]](using Impl)
    extends Constraint[ByteVector, Length[C]]:
  override inline def test(value: ByteVector): Boolean =
    summonInline[Impl].test(value.size)
  override inline def message: String =
    s"Length: (${summonInline[Impl].message})"
  
@SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
inline given [C, Impl <: Constraint[Long, C]](using
    inline impl: Impl,
): LengthByteVector[C, Impl] = new LengthByteVector[C, Impl]
