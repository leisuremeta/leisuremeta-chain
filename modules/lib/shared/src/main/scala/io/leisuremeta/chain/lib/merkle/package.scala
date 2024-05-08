package io.leisuremeta.chain.lib
package merkle

import scala.compiletime.{summonInline}

import cats.syntax.either.*

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.collection.*
import io.github.iltotore.iron.constraint.numeric.*

import scodec.bits.{BitVector, ByteVector}

import codec.byte.{ByteDecoder, ByteEncoder}
import codec.byte.ByteEncoder.ops.*
import datatype.BigNat
import failure.DecodingFailure
import util.iron.given

opaque type Nibbles = BitVector :| Length[Multiple[4L]]

object Nibbles:
  def unsafeFrom(bits: BitVector): Nibbles =
    bits.refineUnsafe[Length[Multiple[4L]]]

extension (bitVector: BitVector)
  def refineToNibble: Either[String, Nibbles] =
    bitVector.refineEither[Length[Multiple[4L]]]

extension (byteVector: ByteVector)
  def toNibbles: Nibbles = byteVector.bits.refineUnsafe[Length[Multiple[4L]]]

given nibblesByteEncoder: ByteEncoder[Nibbles] = (nibbles: Nibbles) =>
  BigNat.unsafeFromLong(nibbles.size / 4).toBytes ++ nibbles.bytes

@SuppressWarnings(Array("org.wartremover.warts.TripleQuestionMark"))
given nibblesByteDecoder: ByteDecoder[Nibbles] =
  ByteDecoder[BigNat].flatMap: nibbleSize =>
    val nibbleSizeLong = nibbleSize.toBigInt.toLong
    ByteDecoder
      .fromFixedSizeBytes((nibbleSizeLong + 1) / 2): nibbleBytes =>
        val bitsSize = nibbleSizeLong * 4
        val padSize  = bitsSize - nibbleBytes.size * 8
        val nibbleBits =
          if padSize > 0 then nibbleBytes.bits.padLeft(padSize)
          else nibbleBytes.bits
        nibbleBits.take(bitsSize)
      .emap(_.refineToNibble.leftMap(DecodingFailure(_)))
