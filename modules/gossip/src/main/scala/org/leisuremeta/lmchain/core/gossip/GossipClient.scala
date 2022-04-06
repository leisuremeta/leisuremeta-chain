package org.leisuremeta.lmchain.core
package gossip

import cats.data.EitherT

import crypto.Signature
import model.{Account, Block, NameState, Signed, TokenState, Transaction}
import model.Transaction.Token

trait GossipClient[F[_]] {
  def bestConfirmedBlock: EitherT[F, String, Block.Header]

  def block(blockHash: Block.BlockHash): EitherT[F, String, Option[Block]]

  def nameState(
    blockHash: Block.BlockHash,
    from: Option[Account.Name],
    limit: Option[Int],
    ): EitherT[F, String, List[(Account.Name, NameState)]]

  def tokenState(
    blockHash: Block.BlockHash,
    from: Option[Token.DefinitionId],
    limit: Option[Int],
  ): EitherT[F, String, List[(Token.DefinitionId, TokenState)]]

  def balanceState(
    blockHash: Block.BlockHash,
    from: Option[(Account, Transaction.Input.Tx)],
    limit: Option[Int],
  ): EitherT[F, String, List[(Account, Transaction.Input.Tx)]]

  def newTxAndBlockSuggestions(
    bloomFilter: BloomFilter
  ): EitherT[F, String, (Set[Signed.Tx], Set[Block])]

  def allNewTxAndBlockSuggestions: EitherT[F, String, (Set[Signed.Tx], Set[Block])]

  def newTxAndBlockVotes(
    bloomFilter: BloomFilter,
    knownBlockVoteKeys: Set[(Block.BlockHash, Int)],
  ): EitherT[F, String, (Set[Signed.Tx], Map[(Block.BlockHash, Int), Signature])]

  def allNewTxAndBlockVotes(
    knownBlockVoteKeys: Set[(Block.BlockHash, Int)],
  ): EitherT[F, String, (Set[Signed.Tx], Map[(Block.BlockHash, Int), Signature])]

  def txs(txHashes: Set[Signed.TxHash]): EitherT[F, String, Set[Signed.Tx]]

}
