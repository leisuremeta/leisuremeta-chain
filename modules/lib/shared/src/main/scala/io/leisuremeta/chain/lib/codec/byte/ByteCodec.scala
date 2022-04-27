package io.leisuremeta.chain.lib
package codec.byte

import scodec.bits.ByteVector
import failure.DecodingFailure

trait ByteCodec[A] extends ByteDecoder[A] with ByteEncoder[A]

object ByteCodec:

  def apply[A](implicit bc: ByteCodec[A]): ByteCodec[A] = bc

  given [A](using
      decoder: ByteDecoder[A],
      encoder: ByteEncoder[A],
  ): ByteCodec[A] = new ByteCodec[A]:
    override def decode(
        bytes: ByteVector,
    ): Either[DecodingFailure, DecodeResult[A]] = decoder.decode(bytes)
    override def encode(a: A): ByteVector = encoder.encode(a)
