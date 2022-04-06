package org.leisuremeta.lmchain.core
package codec.circe

import cats.Eq
import cats.derived.auto.eq._
import cats.implicits._

import eu.timepit.refined.cats._
import io.circe.generic.auto._
import io.circe.refined._
import io.circe.{Decoder, Encoder}
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Prop}
import scodec.bits.ByteVector

import datatype._
import model._

class ByteCodecTest extends munit.ScalaCheckSuite with ModelArbitrary {

  implicit val ByteVectorEqInstance: Eq[ByteVector] = Eq.fromUniversalEquals

  def successfulRoundTrip[A: Arbitrary: Eq: Decoder: Encoder]: Prop = forAll {
    (value: A) =>
      val encoded      = Encoder[A].apply(value)
      val decodeResult = Decoder[A].decodeJson(encoded)
      val isMatched    = decodeResult === Either.right(value)
      if (!isMatched) {
        println(s"===> value: $value")
        println(s"===> encoded: $encoded")
        println(s"===> decode result: $decodeResult")
      }
      isMatched
  }

  property("circe: successful rount trip of NetworkId") {

    successfulRoundTrip[NetworkId]
  }

  property("circe: successful rount trip of UInt256BigInt") {
    successfulRoundTrip[UInt256BigInt]
  }

  property("circe: successful rount trip of UInt256Bytes") {
    successfulRoundTrip[UInt256Bytes]
  }

  property("circe: successful rount trip of Address") {
    successfulRoundTrip[Address]
  }

  property("circe: successful rount trip of Account") {
    successfulRoundTrip[Account]
  }

  property("circe: successful rount trip of Transaction") {
    successfulRoundTrip[Transaction]
  }

//  property("circe: successful rount trip of Block") {
//    import crypto.Hash.circeValueDecoder
    
//    implicitly[Encoder[Option[BigNat]]]
//    implicitly[Encoder[Transaction.Input.Tx]]
//    implicitly[Encoder[Block.Header]]
//    successfulRoundTrip[Block]
//    assertEquals(true, true)
//  }
//  property("circe: successful rount trip of NodeStatus") {
//    successfulRoundTrip[NodeStatus]
//  }
}
