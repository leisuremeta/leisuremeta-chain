package io.leisuremeta.chain.lmscan
package backend2
import com.typesafe.config.ConfigFactory
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import doobie.util.transactor.Transactor

val config = ConfigFactory.load()
val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
  config.getString("ctx.db_className"), // driver classname
  config.getString("ctx.db_url"),       // connect URL (driver-specific)
  config.getString("ctx.db_user"),      // user
  config.getString("ctx.db_pass"),      // password
)
