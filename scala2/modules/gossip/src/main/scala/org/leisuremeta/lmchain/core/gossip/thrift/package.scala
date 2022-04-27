package org.leisuremeta.lmchain.core.gossip

import java.nio.ByteBuffer

import scodec.bits.{BitVector, ByteVector}

package object thrift {
  implicit class BitVectorOps(bitVector: BitVector) {
    def toMutableByteBuffer: ByteBuffer = {
      ByteBuffer.wrap(bitVector.toByteVector.toArray)
    }
  }

  implicit class ByteVectorOps(byteVector: ByteVector) {
    def toMutableByteBuffer: ByteBuffer = {
      ByteBuffer.wrap(byteVector.toArray)
    }
  }
}
