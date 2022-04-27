package org.leisuremeta.lmchain.core
package crypto

import cats.Contravariant

import io.circe.{Decoder, Encoder}
import scodec.bits.ByteVector
import shapeless.tag._

import codec.byte.ByteEncoder
import codec.circe._
import datatype.{UInt256Bytes, UInt256Refine}
import model._

trait Hash[A] {
  def apply(a: A): Hash.Value[A]
  def contramap[B](f: B => A): Hash[B] = (b: B) =>
    shapeless.tag[B][UInt256Bytes](apply(f(b)))
}

object Hash {

  def apply[A](implicit hash: Hash[A]): Hash[A] = hash

  type Value[A] = UInt256Bytes @@ A

  implicit def circeValueDecoder[A]: Decoder[Value[A]] =
    taggedDecoder[UInt256Bytes, A]

  implicit def circeValueEncoder[A]: Encoder[Value[A]] =
    taggedEncoder[UInt256Bytes, A]

  object Value {
    def apply[A](uint256Bytes: UInt256Bytes): Value[A] =
      shapeless.tag[A][UInt256Bytes](uint256Bytes)
  }

  object ops {
    implicit class HashOps[A](val a: A) extends AnyVal {
      def toHash(implicit h: Hash[A]): Value[A] = h(a)
    }
  }

  implicit val contravariant: Contravariant[Hash] = new Contravariant[Hash] {
    override def contramap[A, B](fa: Hash[A])(f: B => A): Hash[B] =
      fa.contramap(f)
  }

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  private def hash[A: ByteEncoder](a: A): Value[A] = {
    val bytes = ByteEncoder[A].encode(a)
    val h     = ByteVector.view(CryptoOps.keccak256(bytes.toArray))
    shapeless.tag[A][UInt256Bytes](UInt256Refine.from(h).toOption.get)
  }

  implicit val blockHeaderHash: Hash[Block.Header] = hash[Block.Header]

  implicit val blockHash: Hash[Block] = Hash[Block.Header].contramap(_.header)

  implicit val transactionHash: Hash[Transaction] = hash[Transaction]

  implicit def merkleTrieNodeHash[K, V]: Hash[MerkleTrieNode[K, V]] =
    hash[MerkleTrieNode[K, V]]

  implicit val publicKeyHash: Hash[PublicKey] = hash[PublicKey]
}
