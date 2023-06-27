package io.leisuremeta.chain.lmscan
package backend2
import com.typesafe.config.ConfigFactory
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import doobie.util.transactor.Transactor
import doobie.*
import doobie.implicits.*
import doobie.hikari.HikariTransactor
import scala.concurrent.ExecutionContext
import com.zaxxer.hikari.HikariConfig
import scala.util.chaining.*

val connectEC: ExecutionContext = ExecutionContext.global

// val a = config.getString("ctx.db_className")

val config2 = ConfigFactory.load()
val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
  config2.getString("ctx.db_className"), // driver classname
  config2.getString("ctx.db_url"),       // connect URL (driver-specific)
  config2.getString("ctx.db_user"),      // user
  config2.getString("ctx.db_pass"),      // password
)

val transactor =
  for
    hikariConfig <- Resource.pure {
      // For the full list of hikari configurations see https://github.com/brettwooldridge/HikariCP#gear-configuration-knobs-baby
      val config = new HikariConfig()
      config.setDriverClassName(config2.getString("ctx.db_className"))
      config.setJdbcUrl(config2.getString("ctx.db_url"))
      config.setUsername(config2.getString("ctx.db_user"))
      config.setPassword(config2.getString("ctx.db_pass"))
      config
    }
    xa <- HikariTransactor.fromHikariConfig[IO](hikariConfig, connectEC)
  yield xa
