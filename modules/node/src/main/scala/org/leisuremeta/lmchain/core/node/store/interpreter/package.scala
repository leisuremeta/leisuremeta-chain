package org.leisuremeta.lmchain.core
package node.store

import scodec.bits.ByteVector
import swaydb.data.slice.Slice
import swaydb.serializers.Serializer
import codec.byte._

package object interpreter {

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  implicit def byteCodecToSerialize[A: ByteCodec]: Serializer[A] =
    new Serializer[A] {
      override def write(data: A): Slice[Byte] =
        Slice[Byte](ByteEncoder[A].encode(data).toArray)
      override def read(data: Slice[Byte]): A =
        ByteDecoder[A].decode(ByteVector view data.toArray) match {
          case Right(DecodeResult(value, remainder)) if remainder.isEmpty =>
            value
          case anything =>
            throw new Exception(s"Fail to decode $data: $anything")
        }
    }
}
