package io.leisuremeta.chain
package api.model

import java.time.Instant

import cats.kernel.Eq

import lib.crypto.{Hash, Signature}
import lib.merkle.MerkleTrieNode
import lib.datatype.BigNat

final case class Block(
    header: Block.Header,
    transactionHashes: Set[Signed.TxHash],
    votes: Set[Signature],
)

object Block:

  type BlockHash = Hash.Value[Block]

  final case class Header(
      number: BigNat,
      parentHash: BlockHash,
      stateRoot: StateRoot,
      transactionsRoot: Option[MerkleTrieNode.MerkleRoot[Signed.TxHash, Unit]],
      timestamp: Instant,
  )

  object Header:
    given eqHeader: Eq[Header] = Eq.fromUniversalEquals

    given headerHash: Hash[Header] = Hash.build
  object ops:
    extension (blockHash: Hash.Value[Block])
      def toHeaderHash: Hash.Value[Header] =
        Hash.Value[Header](blockHash.toUInt256Bytes)

    extension (headerHash: Hash.Value[Header])
      def toBlockHash: Hash.Value[Block] =
        Hash.Value[Block](headerHash.toUInt256Bytes)

  given blockHash: Hash[Block] = Header.headerHash.contramap(_.header)
