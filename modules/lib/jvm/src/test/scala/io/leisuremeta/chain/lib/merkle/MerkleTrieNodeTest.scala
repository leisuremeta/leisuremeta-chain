package io.leisuremeta.chain.lib
package merkle

import eu.timepit.refined.refineV
import codec.byte.{ByteDecoder, ByteEncoder, DecodeResult}
import crypto.Hash
import datatype.UInt256
import util.refined.bitVector.given

import minitest.SimpleTestSuite
import hedgehog.minitest.HedgehogSupport
import hedgehog.*
import scodec.bits.ByteVector

object MerkleTrieNodeTest extends SimpleTestSuite with HedgehogSupport:

  def genPrefix: Gen[MerkleTrieNode.Prefix] = for
    prefixByteArray <- Gen.bytes(Range.linear(0, 64))
    prefixBytes = ByteVector.view(prefixByteArray)
    prefixBits <- Gen.boolean.map {
      case true  => prefixBytes.bits
      case false => prefixBytes.bits.dropRight(4)
    }
  yield refineV[MerkleTrieNode.PrefixCondition](prefixBits).toOption.get

  def genChildren[K, V]: Gen[MerkleTrieNode.Children[K, V]] = Gen
    .list[Option[MerkleTrieNode.MerkleHash[K, V]]](
      Gen.frequency1(
        1 -> Gen.constant(None),
        9 -> Gen.bytes(Range.singleton(32)).map { (byteArray) =>
          UInt256.from(ByteVector.view(byteArray)).toOption.map {
            Hash.Value[MerkleTrieNode[K, V]](_)
          }
        },
      ),
      Range.singleton(16),
    )
    .map((list) =>
      refineV[MerkleTrieNode.ChildrenCondition](list.toVector).toOption.get,
    )

  def genValue: Gen[ByteVector] = Gen.sized(size =>
    Gen.bytes(Range.linear(0, size.value.abs)),
  ) map ByteVector.view

  def genLeaf[K, V]: Gen[MerkleTrieNode[K, V]] = for
    prefix <- genPrefix
    value  <- genValue
  yield MerkleTrieNode.Leaf(prefix, value)

  def genBranch[K, V]: Gen[MerkleTrieNode[K, V]] = for
    prefix   <- genPrefix
    children <- genChildren[K, V]
  yield MerkleTrieNode.Branch(prefix, children)

  def genBranchWithData[K, V]: Gen[MerkleTrieNode[K, V]] = for
    prefix   <- genPrefix
    children <- genChildren[K, V]
    value    <- genValue
  yield MerkleTrieNode.BranchWithData(prefix, children, value)

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  def genMerkleTrieNode[K, V]: Gen[MerkleTrieNode[K, V]] = for
    prefixByteArray <- Gen.bytes(Range.linear(0, 64))
    prefixBytes = ByteVector.view(prefixByteArray)
    prefixBits <- Gen.boolean.map {
      case true  => prefixBytes.bits
      case false => prefixBytes.bits.dropRight(4)
    }
    prefix = refineV[MerkleTrieNode.PrefixCondition](prefixBits).toOption.get
    node <- Gen.choice1(
      genLeaf[K, V],
      genBranch[K, V],
      genBranchWithData[K, V],
    )
  yield node

  property("roundtrip of MerkleTrieNode byte codec") {
    for node <- genMerkleTrieNode[ByteVector, ByteVector].forAll
    yield
      val encoded =
        ByteEncoder[MerkleTrieNode[ByteVector, ByteVector]].encode(node)

      ByteDecoder[MerkleTrieNode[ByteVector, ByteVector]].decode(encoded) match
        case Right(DecodeResult(decoded, remainder)) =>
          Result.all(
            List(
              decoded ==== node,
              Result.assert(remainder.isEmpty),
            ),
          )
        case Left(error) =>
          println(s"=== error: ${error.msg}")
          println(s"=== encoded: $encoded")
          Result.failure
  }
