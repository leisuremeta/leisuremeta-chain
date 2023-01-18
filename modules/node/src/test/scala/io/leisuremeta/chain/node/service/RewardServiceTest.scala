package io.leisuremeta.chain
package node
package service

import java.time.{DayOfWeek, Instant, ZoneId, ZonedDateTime}
import java.time.temporal.{ChronoUnit, TemporalAdjusters}

import scala.concurrent.duration.*

import cats.data.EitherT
import cats.effect.*
import cats.effect.unsafe.implicits.global

import lib.failure.DecodingFailure
import repository.StateRepository

import hedgehog.*
import hedgehog.munit.HedgehogSuite

class RewardServiceTest extends HedgehogSuite:

//  given testKVStore[K, V]: store.KeyValueStore[IO, K, V] =
//    new store.KeyValueStore[IO, K, V]:
//      private val _map = scala.collection.mutable.Map.empty[K, V]
//      def get(key: K): EitherT[IO, DecodingFailure, Option[V]] =
//        scribe.info(s"===> test kv store: get($key): current: $_map")
//        EitherT.pure[IO, DecodingFailure](_map.get(key))
//      def put(key: K, value: V): IO[Unit] =
//        scribe.info(s"===> test kv store: put($key, $value): current: $_map")
//        IO(_map.put(key, value))
//      def remove(key: K): IO[Unit] =
//        scribe.info(s"===> test kv store: remove($key): current: $_map")
//        IO(_map.remove(key))
//
//  given testStateRepo[K, V]: StateRepository[IO, K, V] =
//    StateRepository.fromStores[IO, K, V]
//
//  test("getLatestRewardInstantBefore") {
//    withMunitAssertions { assertions =>
//
//      val timestamp = Instant.parse("2022-10-06T00:00:00Z")
//      val expected  = Instant.parse("2022-10-02T15:00:00Z")
//
//      assertions.assertEquals(
//        RewardService.getLatestRewardInstantBefore(timestamp),
//        expected,
//      )
//    }
//  }
//
//  test("getWeeklyRefTime") {
//    withMunitAssertions { assertions =>
//
//      val timestamp = Instant.parse("2022-10-02T15:00:00Z")
//      val expected  = Seq(
//        Instant.parse("2022-09-25T15:00:00Z"),
//        Instant.parse("2022-09-26T15:00:00Z"),
//        Instant.parse("2022-09-27T15:00:00Z"),
//        Instant.parse("2022-09-28T15:00:00Z"),
//        Instant.parse("2022-09-29T15:00:00Z"),
//        Instant.parse("2022-09-30T15:00:00Z"),
//        Instant.parse("2022-10-01T15:00:00Z"),
//      )
//
//      assertions.assertEquals(
//        RewardService.getWeeklyRefTime(timestamp),
//        expected,
//      )
//    }
//  }
end RewardServiceTest
