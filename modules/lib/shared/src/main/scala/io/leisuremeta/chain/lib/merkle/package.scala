package io.leisuremeta.chain.lib
package merkle

import scala.compiletime.{summonInline}

import cats.Eq
import cats.syntax.either.*
import cats.syntax.eq.given

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.collection.*
import io.github.iltotore.iron.constraint.numeric.*

import scodec.bits.{BitVector, ByteVector}

import codec.byte.{ByteDecoder, ByteEncoder}
import codec.byte.ByteEncoder.ops.*
import datatype.BigNat
import failure.DecodingFailure
import util.iron.given

opaque type Nibbles = BitVector :| Nibbles.NibbleCond

object Nibbles:
  type NibbleCond = Length[Multiple[4L]]
  val empty: Nibbles = BitVector.empty.assumeNibbles

extension (nibbles: Nibbles)
  def value: BitVector  = nibbles
  def bytes: ByteVector = nibbles.bytes
  def nibbleSize: Long  = nibbles.size / 4L
  def unCons: Option[(Int, Nibbles)] =
    if nibbles.isEmpty then None
    else
      val head = nibbles.value.take(4).toInt(signed = false)
      val tail = nibbles.value.drop(4).assumeNibbles
      Some((head, tail))
  def stripPrefix(prefix: Nibbles): Option[Nibbles] =
    if nibbles.startsWith(prefix) then
      Some(nibbles.drop(prefix.size).assumeNibbles)
    else None

  def <=(that: Nibbles): Boolean =
    val thisBytes = nibbles.bytes
    val thatBytes = that.bytes
    val minSize   = thisBytes.size min thatBytes.size

    (0L `until` minSize)
      .find: i =>
        thisBytes.get(i) =!= thatBytes.get(i)
      .fold(nibbles.value.size <= that.value.size): i =>
        (thisBytes.get(i) & 0xff) <= (thatBytes.get(i) & 0xff)

extension (bitVector: BitVector)
  def refineToNibble: Either[String, Nibbles] =
    bitVector.refineEither[Length[Multiple[4L]]]
  def assumeNibbles: Nibbles = bitVector.assume[Nibbles.NibbleCond]

extension (byteVector: ByteVector)
  def toNibbles: Nibbles = byteVector.bits.refineUnsafe[Length[Multiple[4L]]]

given nibblesByteEncoder: ByteEncoder[Nibbles] = (nibbles: Nibbles) =>
  BigNat.unsafeFromLong(nibbles.size / 4).toBytes ++ nibbles.bytes

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

given nibblesEq: Eq[Nibbles] = Eq.fromUniversalEquals
