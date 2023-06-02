package io.leisuremeta.chain.backend2

import doobie.*
import doobie.implicits.*
import cats.*
import cats.effect.*
import cats.implicits.*
import cats.data.*
import cats.effect.unsafe.implicits.global
import io.leisuremeta.chain.backend2.Loggable.*
import io.leisuremeta.chain.lmscan.backend2.entity.Tx
import io.leisuremeta.chain.lmscan.backend2.entity.Summary
import io.leisuremeta.chain.lmscan.backend2.entity.Nft
import doobie.postgres.implicits.* // list 받을때 필요
import com.typesafe.config.ConfigFactory

val operations1 = 42.pure[ConnectionIO]
val operations2 = sql"select 42".query[Int].unique

case class Person(name: String, age: Int)
val nel = NonEmptyList.of(Person("Bob", 12), Person("Alice", 14))

object DoobieExample extends IOApp.Simple:
  val config = ConfigFactory.load()

  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    config.getString("ctx.db_className"), // driver classname
    config.getString("ctx.db_url"),       // connect URL (driver-specific)
    config.getString("ctx.db_user"),      // user
    config.getString("ctx.db_pass"),      // password
  )

  val queries =
    Map(
      "a" -> sql"select 42"
        .query[Int]
        .to[List],
      "b" -> sql"select * from summary"
        .query[Summary]
        .stream
        .take(5)
        .compile
        .toList,
      "c" -> sql"select hash from tx"
        .query[String]
        .stream
        .take(5)
        .compile
        .toList,
      "d" -> sql"select array['foo',NULL,'baz']"
        .query[List[Option[String]]]
        .stream
        .take(5)
        .compile
        .toList,
      "e" -> sql"select to_addr from tx"
        .query[List[String]]
        .stream
        .take(5)
        .compile
        .toList,
      "f" -> sql"""
        select *
        from tx
      """
        .query[Tx]
        .stream
        .take(5)
        .compile
        .toList,
      "g" -> sql"select * from nft".query[Nft].stream.take(5).compile.toList,
    )

  // val sampleTx = sql"select hash, tx_type from tx"
  //   .query[Tx_1]
  //   .stream
  //   .take(5)
  //   .compile
  //   .toList
  //   .transact(xa)

  def val_run = sql"select * from tx"
    .query[Summary]  // Query0[String]
    .to[List]        // ConnectionIO[List[String]]
    .transact(xa)    // IO[List[String]]
    .unsafeRunSync() // List[String]
    .take(5)         // List[String]
    .foreach(println)

  def genQuery =
    queries("f").transact(xa)

  val run =
    genQuery.log.as(ExitCode.Success)
