package org.leisuremeta.lmchain.core
package gossip
package thrift

import java.nio.ByteBuffer

import cats.data.EitherT
import cats.effect.Async
import cats.implicits._

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Thrift
import com.twitter.finagle.liveness.{
  FailureAccrualFactory,
  FailureAccrualPolicy,
}
import com.twitter.finagle.service.Backoff
import com.twitter.util.{Future, Return, Throw, Try}
import io.catbird.util.effect._
import scodec.bits.ByteVector

import codec.byte.{ByteDecoder, ByteEncoder, DecodeResult}
import crypto.Signature
import model.{Account, Block, NameState, Signed, TokenState, Transaction}
import ThriftGossipClient._

class ThriftGossipClient[F[_]: Async](
    dest: String
) extends GossipClient[F] {
  private def client =
    Thrift.client
      .configured(
        FailureAccrualFactory.Param(() =>
          FailureAccrualPolicy.successRateWithinDuration(
            requiredSuccessRate = 0.01,
            window = 5.minutes,
            markDeadFor = Backoff.const(10.seconds),
            minRequestThreshold = 100,
          )
        )
      )
      .build[GossipService.MethodPerEndpoint](dest)

  def bestConfirmedBlock: EitherT[F, String, Block.Header] = for {
    bestHeader <- client.bestConfirmedBlock().toEitherT[F]("bestConfirmedBlock")
    decoded <- EitherT.fromEither[F](decodeByteBuffer[Block.Header](bestHeader))
  } yield decoded

  def block(blockHash: Block.BlockHash): EitherT[F, String, Option[Block]] =
    for {
      blockOption <- client
        .block(blockHash.toMutableByteBuffer)
        .toEitherT[F]("block")
      decoded <- EitherT.fromEither[F](
        decodeByteBuffer[Option[Block]](blockOption)
      )
    } yield decoded

  def nameState(
      blockHash: Block.BlockHash,
      from: Option[Account.Name],
      limit: Option[Int],
  ): EitherT[F, String, List[(Account.Name, NameState)]] = for {
    items <- client
      .nameState(
        ByteEncoder[(Block.BlockHash, Option[Account.Name], Option[Int])]
          .encode(
            (blockHash, from, limit)
          )
          .toMutableByteBuffer
      )
      .toEitherT[F]("nameState")
    decoded <- EitherT.fromEither[F](
      decodeByteBuffer[List[(Account.Name, NameState)]](items)
    )
  } yield decoded

  def tokenState(
      blockHash: Block.BlockHash,
      from: Option[Transaction.Token.DefinitionId],
      limit: Option[Int],
  ): EitherT[F, String, List[(Transaction.Token.DefinitionId, TokenState)]] =
    for {
      items <- client
        .nameState(
          ByteEncoder[
            (Block.BlockHash, Option[Transaction.Token.DefinitionId], Option[Int])
          ]
            .encode(
              (blockHash, from, limit)
            )
            .toMutableByteBuffer
        )
        .toEitherT[F]("tokenState")
      decoded <- EitherT.fromEither[F](
        decodeByteBuffer[List[(Transaction.Token.DefinitionId, TokenState)]](
          items
        )
      )
    } yield decoded

  def balanceState(
      blockHash: Block.BlockHash,
      from: Option[(Account, Transaction.Input.Tx)],
      limit: Option[Int],
  ): EitherT[F, String, List[(Account, Transaction.Input.Tx)]] = for {
    items <- client
      .nameState(
        ByteEncoder[
          (Block.BlockHash, Option[(Account, Transaction.Input.Tx)], Option[Int])
        ]
          .encode(
            (blockHash, from, limit)
          )
          .toMutableByteBuffer
      )
      .toEitherT[F]("tokenState")
    decoded <- EitherT.fromEither[F](
      decodeByteBuffer[List[(Account, Transaction.Input.Tx)]](items)
    )
  } yield decoded

  def newTxAndBlockSuggestions(
      bloomFilter: BloomFilter
  ): EitherT[F, String, (Set[Signed.Tx], Set[Block])] = {
    client
      .newTxAndBlockSuggestions(
        bloomFilter = bloomFilter.bits.toMutableByteBuffer,
        numberOfItems = bloomFilter.numberOfHash.toShort,
      )
      .toEitherT[F]("newTxAndBlockSuggestions")
      .flatMap(decodeTxAndBlockSuggestions)
  }

  def allNewTxAndBlockSuggestions
      : EitherT[F, String, (Set[Signed.Tx], Set[Block])] = client
    .allNewTxAndBlockSuggestions()
    .toEitherT[F]("allNewTxAndBlockSuggestions")
    .flatMap(decodeTxAndBlockSuggestions)

  private def decodeTxAndBlockSuggestions(
      result: TxAndBlockSuggestion
  ): EitherT[F, String, (Set[Signed.Tx], Set[Block])] =
    EitherT.fromEither[F](
      for {
        txs    <- decodeByteBuffer[Set[Signed.Tx]](result.tx)
        blocks <- decodeByteBuffer[Set[Block]](result.block)
      } yield (txs, blocks)
    )

  def newTxAndBlockVotes(
      bloomFilter: BloomFilter,
      knownBlockVoteKeys: Set[(Block.BlockHash, Int)],
  ): EitherT[
    F,
    String,
    (Set[Signed.Tx], Map[(Block.BlockHash, Int), Signature]),
  ] = client
    .newTxAndBlockVotes(
      bloomFilter = bloomFilter.bits.toMutableByteBuffer,
      numberOfItems = bloomFilter.numberOfHash.toShort,
      knownBlockVoteKeys =
        knownBlockVoteKeys.map { blockVoteKey: (Block.BlockHash, Int) =>
          ByteEncoder[(Block.BlockHash, Int)]
            .encode(blockVoteKey)
            .toMutableByteBuffer
        },
    )
    .toEitherT[F]("newTxAndBlockVotes")
    .flatMap(decodeTxAndBlockVotes)

  def allNewTxAndBlockVotes(
      knownBlockVoteKeys: Set[(Block.BlockHash, Int)]
  ): EitherT[
    F,
    String,
    (Set[Signed.Tx], Map[(Block.BlockHash, Int), Signature]),
  ] = client
    .allNewTxAndBlockVotes(
      knownBlockVoteKeys =
        knownBlockVoteKeys.map { blockVoteKey: (Block.BlockHash, Int) =>
          ByteEncoder[(Block.BlockHash, Int)]
            .encode(blockVoteKey)
            .toMutableByteBuffer
        }
    )
    .toEitherT[F]("allNewTxAndBlockVotes")
    .flatMap(decodeTxAndBlockVotes)

  private def decodeTxAndBlockVotes(
      result: TxAndBlockVote
  ): EitherT[
    F,
    String,
    (Set[Signed.Tx], Map[(Block.BlockHash, Int), Signature]),
  ] =
    EitherT.fromEither[F](
      for {
        txs <- decodeByteBuffer[Set[Signed.Tx]](result.tx)
        votes <- decodeByteBuffer[Map[(Block.BlockHash, Int), Signature]](
          result.vote
        )
      } yield (txs, votes)
    )

  def txs(txHashes: Set[Signed.TxHash]): EitherT[F, String, Set[Signed.Tx]] =
    for {
      result <- client
        .txs(txHashes.map(_.toMutableByteBuffer))
        .toEitherT[F]("txs")
      decodeds <- result.toList.traverse { byteBuffer =>
        EitherT.fromEither[F](decodeByteBuffer[Signed.Tx](byteBuffer))
      }
    } yield decodeds.toSet
}
object ThriftGossipClient {

  implicit class FutureOps[A](val fa: Future[A]) extends AnyVal {
    def toEitherT[F[_]: Async](description: String): EitherT[F, String, A] =
      EitherT(
        Async[F]
          .async { (k: (Either[Throwable, A] => Unit)) =>
            fa.respond {
              case Return(a)  => k(Right[Throwable, A](a))
              case Throw(err) => k(Left[Throwable, A](err))
            }
            ()
          }
          .attempt
          .map(_.leftMap { (t: Throwable) =>
            t.formatted(
              s"$description: %s: %s: %s"
                .format(t.getClass.getName, t.getMessage, t.getCause)
            )
          })
      )
  }

  def decodeByteBuffer[A: ByteDecoder](
      byteBuffer: ByteBuffer
  ): Either[String, A] = {
    ByteDecoder[A].decode(ByteVector.view(byteBuffer)) match {
      case Right(DecodeResult(a, remainder)) if remainder.isEmpty =>
        Right(a)
      case Right(DecodeResult(a, remainder)) =>
        Left(s"Decode to $a, but remainder exists: $remainder")
      case value @ Left(failure) =>
        Left(s"Could not decode $value: ${failure.msg}")
    }
  }
}
