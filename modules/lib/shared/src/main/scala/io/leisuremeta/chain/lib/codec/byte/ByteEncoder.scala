package io.leisuremeta.chain.lib.codec.byte

import scodec.bits.ByteVector

trait ByteEncoder[A]:
  def encode(a: A): ByteVector

  def contramap[B](f: B => A): ByteEncoder[B] = b => encode(f(b))

object ByteEncoder:
  def apply[A: ByteEncoder]: ByteEncoder[A] = summon
