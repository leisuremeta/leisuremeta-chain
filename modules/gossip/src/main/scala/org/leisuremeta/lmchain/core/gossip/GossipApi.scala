package org.leisuremeta.lmchain.core
package gossip

import crypto.Signature
import model.{Account, Block, NameState, Signed, TokenState, Transaction}
import model.Transaction.Token

trait GossipApi[F[_]] {
  def bestConfirmedBlock: F[Block.Header]

  def block(blockHash: Block.BlockHash): F[Option[Block]]

  def nameState(
      blockHash: Block.BlockHash,
      from: Option[Account.Name],
      limit: Option[Int],
  ): F[List[(Account.Name, NameState)]]

  def tokenState(
      blockHash: Block.BlockHash,
      from: Option[Token.DefinitionId],
      limit: Option[Int],
  ): F[List[(Token.DefinitionId, TokenState)]]

  def balanceState(
      blockHash: Block.BlockHash,
      from: Option[(Account, Transaction.Input.Tx)],
      limit: Option[Int],
  ): F[List[(Account, Transaction.Input.Tx)]]

  def newTxAndBlockSuggestions(
      bloomFilter: BloomFilter
  ): F[(Set[Signed.Tx], Set[Block])]

  def allNewTxAndBlockSuggestions: F[(Set[Signed.Tx], Set[Block])]

  def newTxAndBlockVotes(
      bloomFilter: BloomFilter,
      knownBlockVoteKeys: Set[(Block.BlockHash, Int)],
  ): F[(Set[Signed.Tx], Map[(Block.BlockHash, Int), Signature])]

  def allNewTxAndBlockVotes(
      knownBlockVoteKeys: Set[(Block.BlockHash, Int)],
  ): F[(Set[Signed.Tx], Map[(Block.BlockHash, Int), Signature])]

  def txs(txHashes: Set[Signed.TxHash]): F[Set[Signed.Tx]]

}
