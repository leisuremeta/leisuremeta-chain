package org.leisuremeta.lmchain.core
package model

import java.time.Instant

import cats.kernel.Eq

import crypto.{Hash, MerkleTrieNode, Signature}
import datatype.{BigNat, UInt256Bytes}
import MerkleTrieNode.MerkleRoot
import Transaction.Token.DefinitionId

final case class Block(
    header: Block.Header,
    transactionHashes: Set[Signed.TxHash],
    votes: Set[Signature],
)

object Block {

  type BlockHash = Hash.Value[Block]

  final case class Header(
      number: BigNat,
      parentHash: BlockHash,
      namesRoot: Option[MerkleRoot[Account.Name, NameState]],
      tokenRoot: Option[MerkleRoot[DefinitionId, TokenState]],
      balanceRoot: Option[MerkleRoot[(Account, Transaction.Input.Tx), Unit]],
      transactionsRoot: Option[MerkleRoot[Signed.TxHash, Unit]],
      timestamp: Instant,
  )

  object Header {
    implicit val eqHeader: Eq[Header] = Eq.fromUniversalEquals
  }

  object ops {
    implicit class BlockHashOps(val blockHash: Hash.Value[Block])
        extends AnyVal {
      def toHeaderHash: Hash.Value[Header] =
        shapeless.tag[Header][UInt256Bytes](blockHash)
    }

    implicit class BlockHeaderHashOps(val headerHash: Hash.Value[Header])
        extends AnyVal {
      def toBlockHash: Hash.Value[Block] =
        shapeless.tag[Block][UInt256Bytes](headerHash)
    }
  }
}
