package io.leisuremeta.chain
package node.service

import java.time.Instant

import scala.concurrent.duration.*

import cats.{Monad, Parallel}
import cats.data.{EitherT, NonEmptySeq, OptionT}
import cats.effect.{Clock, Resource, Spawn, Sync, Temporal}
import cats.effect.syntax.concurrent.*
import cats.effect.syntax.spawn.*
import cats.implicits.*

import api.model.{Block, Signed}
import lib.datatype.UInt256Bytes

object PeriodicActionService:

  def periodicAction[F[_]: Monad: Clock: Spawn: Temporal: LocalGossipService](
      timeWindowMillis: Long,
      numberOfNodes: Int,
      localNodeIndex: Int,
  ): Resource[F, F[Unit]] =
    scribe.info(s"LocalGossipService instance: ${summon[LocalGossipService[F]]}")
    blockSuggestionLoop(timeWindowMillis, numberOfNodes, localNodeIndex)

  def blockSuggestionLoop[F[_]
    : Monad: Clock: Spawn: Temporal: LocalGossipService](
      timeWindowMillis: Long,
      numberOfNodes: Int,
      localNodeIndex: Int,
  ): Resource[F, F[Unit]] =

    val loop: F[Unit] = for
      _ <- Monad[F].pure(scribe.info(s"Starting block suggestion loop"))
      currentTime <- Clock[F].realTime
      nextBlockSuggestionTime = nextBlockSuggestionTimeMillis(
        numberOfNodes,
        localNodeIndex,
        currentTime.toMillis,
        timeWindowMillis,
      )
      _ <- Monad[F].pure(
        scribe.info(
          s"current               time: $currentTime",
        ),
      )
      _ <- Monad[F].pure(
        scribe.info(
          s"next block suggestion time: $nextBlockSuggestionTime",
        ),
      )
      _ <- Monad[F].pure(
        scribe.info(
          s"waiting for           time: ${nextBlockSuggestionTime - currentTime.toMillis}",
        ),
      )
      _ <- periodic[F](
        nextBlockSuggestionTime - currentTime.toMillis,
        timeWindowMillis * numberOfNodes,
      ) {
        for
          localGossip    <- LocalGossipService[F].get
          txHashes = localGossip.newTxs.keySet
          currentTime <- Clock[F].realTimeInstant
          _ <- Monad[F].pure(scribe.info(s"current localGossip: $localGossip"))
          _ <- Monad[F].pure(scribe.info(s"current txHashes: $txHashes"))
          _ <- Monad[F].pure(scribe.info(s"current time: $currentTime"))
          result <- LocalGossipService[F]
            .generateNewBlockSuggestion(currentTime)
            .value
          _ <- Monad[F].pure(
            scribe.info(s"block suggestion loop result: $result"),
          )
        yield ()
      }
    yield ()

    loop.background.map(_.map(_ => ()))
  end blockSuggestionLoop

  def nextBlockSuggestionTimeMillis(
      numberOfNodes: Int,
      localNodeIndex: Int,
      currentTimeMillis: Long,
      timeWindowMillis: Long,
  ): Long =
    val currentTimeWindowNumber = currentTimeMillis / timeWindowMillis
    val currentNodeIndex        = currentTimeWindowNumber % numberOfNodes
    val numberOfWindowsToWait =
      ((localNodeIndex + numberOfNodes) - currentNodeIndex) % numberOfNodes match
        case 0 => // 0인 경우는 현재 이미 자기차례 time window에 있어서 이번 턴은 포기하고 다음 턴을 기다림
          numberOfNodes.toLong
        case n => n
    (currentTimeWindowNumber + numberOfWindowsToWait) * timeWindowMillis

  def periodic[F[_]: Clock: Spawn: Temporal](
      initialDelay: Long,
      interval: Long,
  )(action: F[Unit]): F[Unit] =

    def periodicRun(startTime: Long)(action: F[Unit]): Resource[F, Unit] = for
      now <- Resource.eval(Clock[F].realTime)
      _   <- Resource.eval(Temporal[F].sleep((startTime - now.toMillis).millis))
      _   <- action.background
      _   <- periodicRun(startTime + interval)(action)
    yield ()

    for
      now <- Clock[F].realTime
      _   <- periodicRun(now.toMillis + initialDelay)(action).useForever
    yield ()
  end periodic
