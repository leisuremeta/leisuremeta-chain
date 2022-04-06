package org.leisuremeta.lmchain.core
package gossip

import scala.util.hashing.MurmurHash3
import cats.data.NonEmptySeq
import scodec.bits.BitVector
import datatype.{BigNat, UInt256Bytes}

final case class BloomFilter(bits: BitVector, numberOfHash: Int) {
  def check(keccak256: UInt256Bytes): Boolean =
    BloomFilter.hashes(numberOfHash)(keccak256) forall bits.get
}

object BloomFilter {

  val NumberOfBits: Int = 8192

  def from(keccak256s: NonEmptySeq[UInt256Bytes]): BloomFilter = {

    val numberOfHash = math.round(math.log(2) * NumberOfBits / keccak256s.length).toInt min 20

    val hashValues: Seq[Long] = keccak256s.toSeq flatMap hashes(numberOfHash)

    BloomFilter(hashValues.foldLeft(BitVector.low(NumberOfBits.toLong))(_ set _), numberOfHash)
  }

  private def hashes(numberOfHash: Int)(keccak256: UInt256Bytes): Seq[Long] = for {
    i <- 0 until numberOfHash
    init = keccak256.take(4).toInt()
    murmur = MurmurHash3.bytesHash(keccak256.toArray)
  } yield ((init + i * murmur) % NumberOfBits + NumberOfBits) % NumberOfBits.toLong
}
