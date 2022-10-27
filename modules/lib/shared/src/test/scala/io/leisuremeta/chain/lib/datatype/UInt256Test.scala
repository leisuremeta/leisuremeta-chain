package io.leisuremeta.chain.lib.datatype

import hedgehog.munit.HedgehogSuite
import hedgehog.*
import scodec.bits.ByteVector

class UInt256Test extends HedgehogSuite:

  property("roundtrip of uint256bytes") {
    for bytes <- Gen.bytes(Range.singleton(32)).map(ByteVector.view).forAll
    yield
      val roundTrip =
        for uint256bytes <- UInt256.from(bytes)
        yield uint256bytes.toBytes

      roundTrip ==== Right(bytes)
  }

  property("roundtrip of uint256bigint") {
    for bigint <- Gen.bytes(Range.singleton(32)).map(BigInt(1, _)).forAll
    yield
      val roundTrip =
        for uint256bigint <- UInt256.from(bigint)
        yield uint256bigint.toBigInt

      roundTrip ==== Right(bigint)
  }

  property("roundtrip of uint256bytes -> uint256bigint -> uint256bytes") {
    for bytes <- Gen.bytes(Range.singleton(32)).map(ByteVector.view).forAll
    yield
      val roundTrip =
        for
          uint256bytes <- UInt256.from(bytes)
          bigint = uint256bytes.toBigInt
          uint256bigint <- UInt256.from(bigint)
        yield uint256bigint.toBytes

      roundTrip ==== Right(bytes)
  }

  property("roundtrip of uint256bigint -> uint256bytes -> uint256bigint") {
    for bigint <- Gen.bytes(Range.singleton(32)).map(BigInt(1, _)).forAll
    yield
      val roundTrip =
        for
          uint256bigint <- UInt256.from(bigint)
          bytes = uint256bigint.toBytes
          uint256bytes <- UInt256.from(bytes)
        yield uint256bytes.toBigInt

      roundTrip ==== Right(bigint)
  }
