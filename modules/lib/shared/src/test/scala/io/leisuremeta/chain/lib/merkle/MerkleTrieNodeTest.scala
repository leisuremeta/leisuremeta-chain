package io.leisuremeta.chain.lib
package merkle

import eu.timepit.refined.refineV
import scodec.bits.ByteVector

import hedgehog.*
import hedgehog.munit.HedgehogSuite

import codec.byte.{ByteDecoder, ByteEncoder, DecodeResult}
import crypto.Hash
import datatype.UInt256
import util.refined.bitVector.given

class MerkleTrieNodeTest extends HedgehogSuite:

  def genPrefix: Gen[MerkleTrieNode.Prefix] = for
    prefixByteArray <- Gen.bytes(Range.linear(0, 64))
    prefixBytes = ByteVector.view(prefixByteArray)
    prefixBits <- Gen.boolean.map {
      case true  => prefixBytes.bits
      case false => prefixBytes.bits.dropRight(4)
    }
  yield refineV[MerkleTrieNode.PrefixCondition](prefixBits).toOption.get

  def genChildren: Gen[MerkleTrieNode.Children] = Gen
    .list[Option[MerkleTrieNode.MerkleHash]](
      Gen.frequency1(
        1 -> Gen.constant(None),
        9 -> Gen.bytes(Range.singleton(32)).map { (byteArray) =>
          UInt256.from(ByteVector.view(byteArray)).toOption.map {
            Hash.Value[MerkleTrieNode](_)
          }
        },
      ),
      Range.singleton(16),
    )
    .map((list) =>
      refineV[MerkleTrieNode.ChildrenCondition](list.toVector).toOption.get,
    )

  def genValue: Gen[ByteVector] =
    Gen.sized(size => Gen.bytes(Range.linear(0, size.value.abs)),
    ) map ByteVector.view

  def genLeaf: Gen[MerkleTrieNode] = for
    prefix <- genPrefix
    value  <- genValue
  yield MerkleTrieNode.Leaf(prefix, value)

  def genBranch: Gen[MerkleTrieNode] = for
    prefix   <- genPrefix
    children <- genChildren
  yield MerkleTrieNode.Branch(prefix, children)

  def genBranchWithData: Gen[MerkleTrieNode] = for
    prefix   <- genPrefix
    children <- genChildren
    value    <- genValue
  yield MerkleTrieNode.BranchWithData(prefix, children, value)

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  def genMerkleTrieNode: Gen[MerkleTrieNode] = for
    prefixByteArray <- Gen.bytes(Range.linear(0, 64))
    prefixBytes = ByteVector.view(prefixByteArray)
    prefixBits <- Gen.boolean.map {
      case true  => prefixBytes.bits
      case false => prefixBytes.bits.dropRight(4)
    }
    prefix = refineV[MerkleTrieNode.PrefixCondition](prefixBits).toOption.get
    node <- Gen.choice1(
      genLeaf,
      genBranch,
      genBranchWithData,
    )
  yield node

  property("roundtrip of MerkleTrieNode byte codec") {
    for node <- genMerkleTrieNode.forAll
    yield
      val encoded =
        ByteEncoder[MerkleTrieNode].encode(node)

      ByteDecoder[MerkleTrieNode].decode(encoded) match
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
