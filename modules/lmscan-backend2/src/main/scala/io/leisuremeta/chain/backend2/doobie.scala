package io.leisuremeta.chain.backend2

import doobie.*
import doobie.implicits.*
import cats.*
import cats.effect.*
import cats.implicits.*
import cats.data.*

import cats.effect.unsafe.implicits.global

val operations1 = 42.pure[ConnectionIO]
val operations2 = sql"select 42".query[Int].unique

case class Person(name: String, age: Int)
val nel = NonEmptyList.of(Person("Bob", 12), Person("Alice", 14))

implicit class Loggable[A](io: IO[A]):
  def log: IO[A] = io.flatMap { a =>
    IO {
      println(s"[log] ${a}")
    }.map(_ => a)
  }
  def logM(msg: String = "DEBUG"): IO[A] = io.flatMap { a =>
    IO {
      println(s"[$msg] ${a}")
    }.map(_ => a)
  }

object DoobieExample extends IOApp.Simple:

  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.ds.PGSimpleDataSource",   // driver classname
    "jdbc:postgresql://localhost:54320/scan", // connect URL (driver-specific)
    "playnomm",                               // user
    "dnflskfk0423!",                          // password
  )

  def genQuery =
    sql"select 42".query[Int].to[List].transact(xa)

  val run =
    genQuery.log.as(ExitCode.Success)
