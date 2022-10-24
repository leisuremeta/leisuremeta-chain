package io.leisuremeta.chain.lib
package codec.byte

import scodec.bits.ByteVector
import failure.DecodingFailure

trait ByteDecoder[A]:
  def decode(bytes: ByteVector): Either[DecodingFailure, DecodeResult[A]]

  def map[B](f: A => B): ByteDecoder[B] = bytes =>
    decode(bytes).map { case DecodeResult(a, remainder) =>
      DecodeResult(f(a), remainder)
    }

  def emap[B](f: A => Either[DecodingFailure, B]): ByteDecoder[B] = bytes =>
    for
      decoded   <- decode(bytes)
      converted <- f(decoded.value)
    yield DecodeResult(converted, decoded.remainder)

  def flatMap[B](f: A => ByteDecoder[B]): ByteDecoder[B] = bytes =>
    decode(bytes).flatMap { case DecodeResult(a, remainder) =>
      f(a).decode(remainder)
    }

final case class DecodeResult[+A](value: A, remainder: ByteVector)

object ByteDecoder:
  def apply[A: ByteDecoder]: ByteDecoder[A] = summon
