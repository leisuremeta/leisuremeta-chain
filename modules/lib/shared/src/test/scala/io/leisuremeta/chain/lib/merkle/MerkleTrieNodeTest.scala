package io.leisuremeta.chain.lib
package merkle

import io.github.iltotore.iron.assume
import scodec.bits.ByteVector

import hedgehog.*
import hedgehog.munit.HedgehogSuite

import codec.byte.{ByteDecoder, ByteEncoder, DecodeResult}
import crypto.Hash
import datatype.UInt256

class MerkleTrieNodeTest extends HedgehogSuite:

  val genPrefix: Gen[Nibbles] = for
    bytes <- Gen.bytes(Range.linear(0, 64))
    byteVector = ByteVector.view(bytes)
    bits <- Gen.element1(
      byteVector.bits,
      byteVector.bits.drop(4),
    )
  yield bits.assumeNibbles

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
    .map: (list) =>
      list.toVector.assume[MerkleTrieNode.ChildrenCondition]

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

  def genMerkleTrieNode: Gen[MerkleTrieNode] = for
    node <- Gen.choice1(
      genLeaf,
      genBranch,
      genBranchWithData,
    )
  yield node

  property("roundtrip of MerkleTrieNode byte codec"):
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
