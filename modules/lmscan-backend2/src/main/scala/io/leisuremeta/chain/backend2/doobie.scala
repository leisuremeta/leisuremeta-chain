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

object DoobieExample extends IOApp.Simple:

  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.ds.PGSimpleDataSource",   // driver classname
    "jdbc:postgresql://localhost:54320/scan", // connect URL (driver-specific)
    "playnomm",                               // user
    "dnflskfk0423",                           // password
  )

//   def genQuery: Int =
//     val query = sql"select 42".query[Int].to[List]
  // query.

//   val io = operations2.transact(transactor).unsafeRunSync()
//   println(io)
  val run =
    IO.println(4)
    // program1
