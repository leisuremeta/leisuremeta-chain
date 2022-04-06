package org.leisuremeta.lmchain.core
package codec.byte

import cats.Eq
import cats.derived.auto.eq._
import cats.implicits._

import eu.timepit.refined.cats._
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Prop}
import scodec.bits.ByteVector

import crypto._
import datatype._
import model._

class ByteCodecTest extends munit.ScalaCheckSuite with ModelArbitrary {

  implicit val ByteVectorEqInstance: Eq[ByteVector] = Eq.fromUniversalEquals

  def successfulRoundTrip[A: Arbitrary: Eq](implicit
      codec: ByteCodec[A]
  ): Prop = forAll { (value: A) =>
    val encoded      = codec.encode(value)
    val decodeResult = codec.decode(encoded)
    val isMatched =
      decodeResult === Either.right(DecodeResult(value, ByteVector.empty))
    if (!isMatched) {
      println(s"===> value: $value")
      println(s"===> encoded: $encoded")
      println(s"===> decode result: $decodeResult")
      decodeResult match {
        case Left(value) => println(s"===> msg: ${value.msg}")
        case Right(_)    => ()
      }
    }
    isMatched
  }

  property("successful rount trip of NetworkId") {
    successfulRoundTrip[NetworkId]
  }

  property("successful rount trip of UInt256BigInt") {
    successfulRoundTrip[UInt256BigInt]
  }

  property("successful rount trip of UInt256Bytes") {
    successfulRoundTrip[UInt256Bytes]
  }

  property("successful rount trip of Utf8") {
    successfulRoundTrip[Utf8]
  }

  property("successful rount trip of KeyPair") {
    successfulRoundTrip[KeyPair]
  }

  property("successful rount trip of PublicKey") {
    successfulRoundTrip[PublicKey]
  }

  property("successful rount trip of Signature") {
    successfulRoundTrip[Signature]
  }

  property("successful rount trip of MerkleTrieNode") {
    successfulRoundTrip[MerkleTrieNode[ByteVector, ByteVector]]
  }

  property("successful rount trip of Transaction") {
    successfulRoundTrip[Transaction]
  }

  property("successful rount trip of NameState") {
    successfulRoundTrip[NameState]
  }

  property("successful rount trip of TokenState") {
    successfulRoundTrip[TokenState]
  }

  property("successful round trip of AccountSignature") {
    successfulRoundTrip[AccountSignature]
  }

  property("successful round trip of Signed[Transaction]") {
    successfulRoundTrip[Signed[Transaction]]
  }

  property("successful rount trip of Block") {
    successfulRoundTrip[Block]
  }
}
