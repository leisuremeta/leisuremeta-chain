package io.leisuremeta.chain.lib
package merkle

import eu.timepit.refined.refineV
import codec.byte.{ByteDecoder, ByteEncoder, DecodeResult}
import crypto.Hash
import datatype.UInt256
import util.refined.bitVector.given

import hedgehog.munit.HedgehogSuite
import hedgehog.*
import scodec.bits.ByteVector

class GenericMerkleTrieNodeTest extends HedgehogSuite:

  def genPrefix: Gen[GenericMerkleTrieNode.Prefix] = for
    prefixByteArray <- Gen.bytes(Range.linear(0, 64))
    prefixBytes = ByteVector.view(prefixByteArray)
    prefixBits <- Gen.boolean.map {
      case true  => prefixBytes.bits
      case false => prefixBytes.bits.dropRight(4)
    }
  yield refineV[GenericMerkleTrieNode.PrefixCondition](prefixBits).toOption.get

  def genChildren[K, V]: Gen[GenericMerkleTrieNode.Children[K, V]] = Gen
    .list[Option[GenericMerkleTrieNode.MerkleHash[K, V]]](
      Gen.frequency1(
        1 -> Gen.constant(None),
        9 -> Gen.bytes(Range.singleton(32)).map { (byteArray) =>
          UInt256.from(ByteVector.view(byteArray)).toOption.map {
            Hash.Value[GenericMerkleTrieNode[K, V]](_)
          }
        },
      ),
      Range.singleton(16),
    )
    .map((list) =>
      refineV[GenericMerkleTrieNode.ChildrenCondition](list.toVector).toOption.get,
    )

  def genValue: Gen[ByteVector] = Gen.sized(size =>
    Gen.bytes(Range.linear(0, size.value.abs)),
  ) map ByteVector.view

  def genLeaf[K, V]: Gen[GenericMerkleTrieNode[K, V]] = for
    prefix <- genPrefix
    value  <- genValue
  yield GenericMerkleTrieNode.Leaf(prefix, value)

  def genBranch[K, V]: Gen[GenericMerkleTrieNode[K, V]] = for
    prefix   <- genPrefix
    children <- genChildren[K, V]
  yield GenericMerkleTrieNode.Branch(prefix, children)

  def genBranchWithData[K, V]: Gen[GenericMerkleTrieNode[K, V]] = for
    prefix   <- genPrefix
    children <- genChildren[K, V]
    value    <- genValue
  yield GenericMerkleTrieNode.BranchWithData(prefix, children, value)

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  def genMerkleTrieNode[K, V]: Gen[GenericMerkleTrieNode[K, V]] = for
    prefixByteArray <- Gen.bytes(Range.linear(0, 64))
    prefixBytes = ByteVector.view(prefixByteArray)
    prefixBits <- Gen.boolean.map {
      case true  => prefixBytes.bits
      case false => prefixBytes.bits.dropRight(4)
    }
    prefix = refineV[GenericMerkleTrieNode.PrefixCondition](prefixBits).toOption.get
    node <- Gen.choice1(
      genLeaf[K, V],
      genBranch[K, V],
      genBranchWithData[K, V],
    )
  yield node

  property("roundtrip of GenericMerkleTrieNode byte codec") {
    for node <- genMerkleTrieNode[ByteVector, ByteVector].forAll
    yield
      val encoded =
        ByteEncoder[GenericMerkleTrieNode[ByteVector, ByteVector]].encode(node)

      ByteDecoder[GenericMerkleTrieNode[ByteVector, ByteVector]].decode(encoded) match
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
