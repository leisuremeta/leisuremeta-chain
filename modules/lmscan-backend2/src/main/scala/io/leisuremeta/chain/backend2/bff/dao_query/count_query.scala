package io.leisuremeta.chain.backend2
import doobie.*
import doobie.implicits.*

import com.typesafe.config.ConfigFactory
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.backend2.Log.log2

import fs2.Stream

val config = ConfigFactory.load()
val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
  config.getString("ctx.db_className"), // driver classname
  config.getString("ctx.db_url"),       // connect URL (driver-specific)
  config.getString("ctx.db_user"),      // user
  config.getString("ctx.db_pass"),      // password
)

object CountQuery:
  def getTxCount() =
    sql"SELECT COUNT(*) FROM Tx "
      .query[Int] // DAO
      .unique
      .transact(xa)
      .attemptSql
