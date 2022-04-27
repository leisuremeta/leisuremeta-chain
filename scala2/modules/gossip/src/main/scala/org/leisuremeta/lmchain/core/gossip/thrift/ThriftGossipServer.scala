package org.leisuremeta.lmchain.core
package gossip
package thrift

import java.nio.ByteBuffer

import cats.effect.{Effect, IO}
import cats.effect.concurrent.Ref
import cats.implicits._

import com.twitter.finagle.{ListeningServer, Thrift}
import com.twitter.util.{Future, Promise}
import io.catbird.util.Rerunnable
import scodec.bits.{BitVector, ByteVector}

import codec.byte.{ByteDecoder, ByteEncoder, DecodeResult}
import crypto.{Hash, Signature}
import datatype.{UInt256Bytes, UInt256Refine}
import failure.DecodingFailure
import model.{Account, Block, NameState, Signed, TokenState, Transaction}
import ThriftGossipServer._

class ThriftGossipServer[F[_]: Effect](
    gossipServer: GossipApi[F]
) extends GossipService.MethodPerEndpoint {
  def bestConfirmedBlock(): Future[ByteBuffer] = {
    val bestBlockEffect: F[ByteBuffer] = for {
      bestBlock <- gossipServer.bestConfirmedBlock
    } yield ByteEncoder[Block.Header].encode(bestBlock).toMutableByteBuffer

    unsafeEffectToFuture(bestBlockEffect)
  }

  def block(blockHash: ByteBuffer): Future[ByteBuffer] = {

    decodeHash[Block](blockHash) match {
      case Left(err) =>
        Future.exception(new IllegalArgumentException(err.toString))
      case Right(blockHashValue) =>
        val blockEffect: F[ByteBuffer] = for {
          blockOption <- gossipServer.block(blockHashValue)
        } yield ByteEncoder[Option[Block]]
          .encode(blockOption)
          .toMutableByteBuffer

        unsafeEffectToFuture(blockEffect)
    }
  }
  def balanceState(argz: ByteBuffer): Future[ByteBuffer] = {
    ByteDecoder[BalanceStateArgs].decode(ByteVector.view(argz)) match {
      case Right(DecodeResult(BalanceStateArgs(blockHash, from, limit), r))
          if r.isEmpty =>
        val balanceStateEffect: F[ByteBuffer] = for {
          items <- gossipServer.balanceState(blockHash, from, limit)
        } yield {
          val bytes =
            ByteEncoder[List[(Account, Transaction.Input.Tx)]].encode(items)
          bytes.toMutableByteBuffer
        }
        unsafeEffectToFuture(balanceStateEffect)
      case _ =>
        Future.exception(
          new IllegalArgumentException(ByteVector.view(argz).toString)
        )
    }
  }
  def nameState(argz: ByteBuffer): Future[ByteBuffer] = {
    ByteDecoder[NameStateArgs].decode(ByteVector.view(argz)) match {
      case Right(DecodeResult(NameStateArgs(blockHash, from, limit), r))
          if r.isEmpty =>
        val nameStateEffect: F[ByteBuffer] = for {
          items <- gossipServer.nameState(blockHash, from, limit)
        } yield {
          val bytes = ByteEncoder[List[(Account.Name, NameState)]].encode(items)
          bytes.toMutableByteBuffer
        }
        unsafeEffectToFuture(nameStateEffect)
      case _ =>
        Future.exception(
          new IllegalArgumentException(ByteVector.view(argz).toString)
        )
    }
  }
  def tokenState(argz: ByteBuffer): Future[ByteBuffer] = {
    ByteDecoder[TokenStateArgs].decode(ByteVector.view(argz)) match {
      case Right(DecodeResult(TokenStateArgs(blockHash, from, limit), r))
          if r.isEmpty =>
        val tokenStateEffect: F[ByteBuffer] = for {
          items <- gossipServer.tokenState(blockHash, from, limit)
        } yield {
          val bytes =
            ByteEncoder[List[(Transaction.Token.DefinitionId, TokenState)]]
              .encode(items)
          bytes.toMutableByteBuffer
        }
        unsafeEffectToFuture(tokenStateEffect)
      case _ =>
        Future.exception(
          new IllegalArgumentException(ByteVector.view(argz).toString)
        )
    }
  }

  def newTxAndBlockSuggestions(
      bloomFilter: ByteBuffer,
      numberOfItems: Short,
  ): Future[TxAndBlockSuggestion] = {

    val newTxAndBlockSuggestionsEffect: F[TxAndBlockSuggestion] =
      gossipServer.newTxAndBlockSuggestions(
        buildBloomFilter(bloomFilter, numberOfItems)
      ) map encodeTxAndBlockSuggestions

    unsafeEffectToFuture(newTxAndBlockSuggestionsEffect)
  }

  private def buildBloomFilter(
      buffer: ByteBuffer,
      numberOfItems: Short,
  ): BloomFilter = {
    val bits = ByteVector.view(buffer).bits
    BloomFilter(bits, numberOfItems.toInt)
  }

  def allNewTxAndBlockSuggestions(): Future[TxAndBlockSuggestion] = {
    val allNewTxAndBlockSuggestionsEffect: F[TxAndBlockSuggestion] =
      gossipServer.allNewTxAndBlockSuggestions map encodeTxAndBlockSuggestions

    unsafeEffectToFuture(allNewTxAndBlockSuggestionsEffect)
  }

  private def encodeTxAndBlockSuggestions(
      txsAndBlocks: (Set[Signed.Tx], Set[Block])
  ): TxAndBlockSuggestion = txsAndBlocks match {
    case (txs, blocks) =>
      TxAndBlockSuggestion(
        tx = ByteEncoder[Set[Signed.Tx]].encode(txs).toMutableByteBuffer,
        block = ByteEncoder[Set[Block]].encode(blocks).toMutableByteBuffer,
      )
  }

  def newTxAndBlockVotes(
      bloomFilter: ByteBuffer,
      numberOfItems: Short,
      knownBlockVoteKeys: scala.collection.Set[ByteBuffer],
  ): Future[TxAndBlockVote] = {
    parseKnownBlockVoteKeysAndGetTxAndBlockVote(knownBlockVoteKeys)(
      gossipServer.newTxAndBlockVotes(
        buildBloomFilter(bloomFilter, numberOfItems),
        _,
      )
    )
  }

  def allNewTxAndBlockVotes(
      knownBlockVoteKeys: scala.collection.Set[ByteBuffer]
  ): Future[TxAndBlockVote] = {
    parseKnownBlockVoteKeysAndGetTxAndBlockVote(knownBlockVoteKeys)(
      gossipServer.allNewTxAndBlockVotes
    )
  }

  private def parseKnownBlockVoteKeysAndGetTxAndBlockVote(
      knownBlockVoteKeys: scala.collection.Set[ByteBuffer]
  )(
      getTxsAndBlockVotes: Set[(Block.BlockHash, Int)] => F[
        (Set[Signed.Tx], Map[(Block.BlockHash, Int), Signature])
      ]
  ): Future[TxAndBlockVote] = {
    def blockVoteKeyDecoded(
        blockVoteKey: ByteBuffer
    ): F[(Block.BlockHash, Int)] = {
      val blockVoteKeyBytes: ByteVector = ByteVector.view(blockVoteKey)
      ByteDecoder[(Block.BlockHash, Int)].decode(blockVoteKeyBytes) match {
        case Right(DecodeResult((blockHash, nodeNumber), r)) if r.isEmpty =>
          Effect[F].pure((blockHash, nodeNumber))
        case _ =>
          Effect[F].raiseError(
            new IllegalArgumentException(blockVoteKeyBytes.toString)
          )
      }
    }

    val knownBlockVoteKeysF: F[Set[(Block.BlockHash, Int)]] =
      knownBlockVoteKeys.toList.traverse(blockVoteKeyDecoded).map(_.toSet)

    val newTxAndBlockVoteEffect: F[TxAndBlockVote] = for {
      knownBlockVoteKeys1 <- knownBlockVoteKeysF
      (txs, votes)        <- getTxsAndBlockVotes(knownBlockVoteKeys1)
    } yield TxAndBlockVote(
      tx = ByteEncoder[Set[Signed.Tx]].encode(txs).toMutableByteBuffer,
      vote = ByteEncoder[Map[(Block.BlockHash, Int), Signature]]
        .encode(votes)
        .toMutableByteBuffer,
    )

    unsafeEffectToFuture(newTxAndBlockVoteEffect)

  }

  def txs(
      txHashes: scala.collection.Set[ByteBuffer]
  ): Future[Set[ByteBuffer]] = {
    val txHashes1: Set[Signed.TxHash] = for {
      (txHash: ByteBuffer) <- txHashes.toSet
      (txHash1: Signed.TxHash) <- decodeHash[Signed.Tx](
        txHash
      ).toOption.toSet
    } yield txHash1

    val txsEffect: F[Set[ByteBuffer]] = for {
      txs <- gossipServer.txs(txHashes1)
    } yield txs.map(ByteEncoder[Signed.Tx].encode(_).toMutableByteBuffer)

    unsafeEffectToFuture(txsEffect)
  }
}

object ThriftGossipServer {

  def serve[F[_]: Effect](
      address: String
  )(gossipServer: GossipApi[F]): ListeningServer = Thrift.server.serveIface(
    addr = address,
    iface = new ThriftGossipServer[F](gossipServer),
  )

  def decodeHash[A](byteBuffer: ByteBuffer): Either[String, Hash.Value[A]] = {
    UInt256Refine.from(ByteVector.view(byteBuffer)).map(Hash.Value[A](_))
  }

  def unsafeEffectToFuture[F[_]: Effect, A](fa: F[A]): Future[A] = {
    val p = Promise[A]()

    Effect[F]
      .runAsync(fa) {
        case Left(ex) => IO(p.setException(ex))
        case Right(x) => IO(p.setValue(x))
      }
      .unsafeRunSync()

    p
  }

  case class NameStateArgs(
      blockHash: Block.BlockHash,
      from: Option[Account.Name],
      limit: Option[Int],
  )

  case class TokenStateArgs(
      blockHash: Block.BlockHash,
      from: Option[Transaction.Token.DefinitionId],
      limit: Option[Int],
  )

  case class BalanceStateArgs(
      blockHash: Block.BlockHash,
      from: Option[(Account, Transaction.Input.Tx)],
      limit: Option[Int],
  )
}
