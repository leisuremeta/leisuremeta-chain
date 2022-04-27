package org.leisuremeta.lmchain.core
package node.service

import java.time.Instant

import scala.concurrent.duration._

import cats.{Monad, Parallel}
import cats.data.{EitherT, NonEmptySeq, OptionT}
import cats.effect.{
  Async,
  Concurrent,
  ConcurrentEffect,
  ContextShift,
  Effect,
  Resource,
  Timer,
}
import cats.effect.syntax.concurrent._
import cats.implicits._

import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable

import datatype.UInt256Bytes
import model.{Block, Signed}
import gossip.{BloomFilter, GossipClient}

object PeriodicActionService {

  def periodicAction[F[_]
    : Parallel: ConcurrentEffect: ContextShift: LocalGossipService: Timer](
      timeWindowMillis: Long,
      numberOfNodes: Int,
      localNodeIndex: Int,
      gossipClients: Map[Int, GossipClient[F]],
  ): Resource[F, F[Unit]] = for {
    f1 <- blockSuggestionLoop(timeWindowMillis, numberOfNodes, localNodeIndex)
    f2 <- gossipPullingLoop(timeWindowMillis, gossipClients)
  } yield ConcurrentEffect[F].tuple2(f1, f2).map(_ => ())

  def blockSuggestionLoop[F[_]
    : ConcurrentEffect: ContextShift: LocalGossipService: Timer](
      timeWindowMillis: Long,
      numberOfNodes: Int,
      localNodeIndex: Int,
  ): Resource[F, F[Unit]] = (for {
    _ <- Effect[F].delay(scribe.debug(s"Starting block suggestion loop"))
    currentTime <- Timer[F].clock.realTime(MILLISECONDS)
    nextBlockSuggestionTime = nextBlockSuggestionTimeMillis(
      numberOfNodes,
      localNodeIndex,
      currentTime,
      timeWindowMillis,
    )
    _ <- Effect[F].delay(
      scribe.debug(
        s"current               time: $currentTime"
      )
    )
    _ <- Effect[F].delay(
      scribe.debug(
        s"next block suggestion time: $nextBlockSuggestionTime"
      )
    )
    _ <- Effect[F].delay(
      scribe.debug(
        s"waiting for           time: ${nextBlockSuggestionTime - currentTime}"
      )
    )
    _ <- periodic[F](
      nextBlockSuggestionTime - currentTime,
      timeWindowMillis * numberOfNodes,
    ) {
      ConcurrentEffect[F]
        .toIO(for {
          txHashes    <- LocalGossipService[F].get.map(_.newTxs.keySet)
          currentTime <- Timer[F].clock.realTime(MILLISECONDS)
          _ <- Effect[F].delay(scribe.debug(s"current txHashes: $txHashes"))
          _ <- Effect[F].delay(scribe.debug(s"current time: $currentTime"))
          result <- LocalGossipService[F]
            .generateNewBlockSuggestion(Instant.ofEpochMilli(currentTime))
            .value
          _ <- Effect[F]
            .delay(scribe.debug(s"block suggestion loop result: $result"))
        } yield ())
        .unsafeRunAsyncAndForget()
    }
  } yield ()).background

  def nextBlockSuggestionTimeMillis(
      numberOfNodes: Int,
      localNodeIndex: Int,
      currentTimeMillis: Long,
      timeWindowMillis: Long,
  ): Long = {
    val currentTimeWindowNumber = currentTimeMillis / timeWindowMillis
    val currentNodeIndex        = currentTimeWindowNumber % numberOfNodes
    val numberOfWindowsToWait =
      ((localNodeIndex + numberOfNodes) - currentNodeIndex) % numberOfNodes match {
        case 0 => // 0인 경우는 현재 이미 자기차례 time window에 있어서 이번 턴은 포기하고 다음 턴을 기다림
          numberOfNodes.toLong
        case n => n
      }
    (currentTimeWindowNumber + numberOfWindowsToWait) * timeWindowMillis
  }

  def gossipPullingLoop[F[_]
    : Parallel: ConcurrentEffect: Timer: LocalGossipService](
      timeWindowMillis: Long,
      gossipClients: Map[Int, GossipClient[F]],
  ): Resource[F, F[Unit]] = (for {
    now <- Timer[F].clock.realTime(MILLISECONDS)
    (t0, t1) = nextGossipPullingTimeMillis(now, timeWindowMillis)
    _ <- Effect[F].delay(
      scribe.debug(s"Starting gossip pulling loop: ($t0, $t1)")
    )
    _ <- traverseClients(gossipClients)(t0)(pullBlockVoteGossip)
    _ <- traverseClients(gossipClients)(t1)(pullBlockSuggestionGossip)
  } yield ()).foreverM.background

  def nextGossipPullingTimeMillis(
      currentTimeMillis: Long,
      timeWindowMillis: Long,
  ): (Long, Long) = {
    val currentTimeWindowNumber = currentTimeMillis / timeWindowMillis
    val nextTimeWindowStarts = (currentTimeWindowNumber + 1) * timeWindowMillis
    (nextTimeWindowStarts, nextTimeWindowStarts + timeWindowMillis / 2)
  }

  def traverseClients[F[_]: Monad: Parallel: Timer](
      clients: Map[Int, GossipClient[F]]
  )(
      startTime: Long
  )(run: GossipClient[F] => EitherT[F, String, Unit]): F[Unit] = {
    for {
      now <- Timer[F].clock.realTime(MILLISECONDS)
      _   <- Timer[F].sleep((startTime - now).millis)
      _ <- clients.toList.parTraverse { case (i, client) =>
        run(client).value.map {
          case Left(errorMsg) => scribe.error(s"node #$i: $errorMsg")
          case Right(_)       => ()
        }
      }
    } yield ()
  }

  def pullBlockSuggestionGossip[F[_]: Effect: LocalGossipService](
      gossipClient: GossipClient[F]
  ): EitherT[F, String, Unit] =
    for {
      _ <- EitherT.rightT[F, String](
        scribe.debug(s"Pulling block suggestion gossip")
      )
      localGossip <- EitherT.right[String](LocalGossipService[F].get)
      txHashes    = localGossip.newTxs.keySet
      blockHashes = localGossip.newBlockSuggestions.keySet
      hashesOption: Option[NonEmptySeq[UInt256Bytes]] = NonEmptySeq.fromSeq(
        txHashes.toSeq ++ blockHashes.toSeq
      )
      (newTxs, newBlockSuggestions) <- hashesOption.fold(
        gossipClient.allNewTxAndBlockSuggestions
      ) { hashes =>
        gossipClient.newTxAndBlockSuggestions(BloomFilter.from(hashes))
      }
      unknownTxHashes <- EitherT
        .right(
          newBlockSuggestions.toList
            .traverse(findUnknownTxHashesFromBlockSuggestion[F])
        )
        .map(_.flatten)
      unknownTxs <- gossipClient.txs(unknownTxHashes.toSet)
      newTxs1 = newTxs ++ unknownTxs
      _ <- EitherT.rightT[F, String](scribe.debug(s"new txs: $newTxs1"))
      _ <- EitherT.rightT[F, String](
        scribe.debug(s"new blocks: $newBlockSuggestions")
      )
      _ <- newTxs1.toList.traverse(LocalGossipService[F].onNewTx(_))
      _ <- newBlockSuggestions.toList
        .traverse(LocalGossipService[F].onNewBlockSuggestion(_))
    } yield ()

  def findUnknownTxHashesFromBlockSuggestion[F[_]: Effect: LocalGossipService](
      blockSuggestion: Block
  ): F[List[Signed.TxHash]] = {
    val txHashes = blockSuggestion.transactionHashes.toList
    txHashes
      .traverse { (txHash: Signed.TxHash) =>
        LocalGossipService[F].get.map(_.newTxs.get(txHash)).map {
          case Some(_) => None
          case None    => Some(txHash)
        }
      }
      .map(_.flatten)
  }

  def pullBlockVoteGossip[F[_]: Effect: LocalGossipService](
      gossipClient: GossipClient[F]
  ): EitherT[F, String, Unit] = for {
    _ <- EitherT.rightT[F, String](scribe.debug(s"Pulling block vote gossip"))
    localGossip <- EitherT.right[String](LocalGossipService[F].get)
    txHashes     = localGossip.newTxs.keySet
    votes        = localGossip.newBlockVotes.keySet
    hashesOption = NonEmptySeq.fromSeq(txHashes.toSeq)
    (newTxs, newBlockVotes) <- hashesOption.fold(
      gossipClient.allNewTxAndBlockVotes(votes)
    ) { hashes =>
      gossipClient.newTxAndBlockVotes(BloomFilter.from(hashes), votes)
    }
    _ <- EitherT.rightT[F, String](scribe.debug(s"new txs: $newTxs"))
    _ <- EitherT.rightT[F, String](
      scribe.debug(s"new votes: ${newBlockVotes.keySet}")
    )
    unknownBlockSuggestionHashes = newBlockVotes.keySet
      .map(_._1)
      .filterNot(
        localGossip.newBlockSuggestions.keySet.contains
      )
    unknownBlockSuggestions <- unknownBlockSuggestionHashes.toList.traverse {
      (blockHash: Block.BlockHash) =>
        OptionT(gossipClient.block(blockHash)).getOrElseF(
          EitherT.leftT[F, Block](s"No block with hash $blockHash")
        )
    }
    _ <- newTxs.toList.traverse(LocalGossipService[F].onNewTx(_))
    _ <- unknownBlockSuggestions.traverse(
      LocalGossipService[F].onNewBlockSuggestion(_)
    )
    _ <- newBlockVotes.toList.traverse {
      case ((blockHash, nodeIndex), sig) =>
        LocalGossipService[F].onNewBlockVote(blockHash, nodeIndex, sig)
    }
  } yield ()

  def periodic[F[_]: Concurrent: ContextShift](
      startTime: Long,
      interval: Long,
  )(action: => Unit): F[Unit] = Async.fromFuture(
    Async[F].delay(
      Observable
        .intervalAtFixedRate(startTime.millis, interval.millis)
        .foreach(_ => action)
    )
  )
}
