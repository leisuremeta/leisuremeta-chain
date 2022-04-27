package io.leisuremeta.chain.lib
package crypto

import minitest.SimpleTestSuite
import hedgehog.minitest.HedgehogSupport
import hedgehog.*

import scodec.bits.*

object CryptoOpsTest extends SimpleTestSuite with HedgehogSupport:

  example("keccak256 #1") {
    val expected =
      hex"c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470"
    val result = ByteVector.view(CryptoOps.keccak256(Array.empty))

    expected ==== result
  }

  example("keccak256 #2") {
    val expected =
      hex"4d741b6f1eb29cb2a9b9911c82f56fa8d73b04959d3d9d222895df6c0b28aa15"
    val result = ByteVector.view(
      CryptoOps.keccak256(
        "The quick brown fox jumps over the lazy dog".getBytes(),
      ),
    )
    expected ==== result
  }

  example("keccak256 #3") {
    val expected =
      hex"578951e24efd62a3d63a86f7cd19aaa53c898fe287d2552133220370240b572d"
    val result = ByteVector.view(
      CryptoOps.keccak256(
        "The quick brown fox jumps over the lazy dog.".getBytes(),
      ),
    )
    expected ==== result
  }

  example("keypair") {
    val keyPair = CryptoOps.fromPrivate(
      BigInt(
        "10e93a6c964aa6bc089f84e4fe3fb37583f3e1162891a689dd99bb629520f3df",
        16,
      ),
    )
    val expected =
      hex"e72699136b12ffd11549616ff047cd5ec93665cd6f13b859030a3c99d14842abc27a7442bc05143db53c41407a7059c85def28f6749b86b3123c48be3085e459"

    expected ==== keyPair.publicKey.toBytes
  }
