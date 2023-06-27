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

val config = ConfigFactory.load()
val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
  config.getString("ctx.db_className"), // driver classname
  config.getString("ctx.db_url"),       // connect URL (driver-specific)
  config.getString("ctx.db_user"),      // user
  config.getString("ctx.db_pass"),      // password
)

val transactor =
  for
    hikariConfig <- Resource.pure {
      // For the full list of hikari configurations see https://github.com/brettwooldridge/HikariCP#gear-configuration-knobs-baby
      val hikariConfig = new HikariConfig()
      hikariConfig.setDriverClassName(config.getString("ctx.db_className"))
      hikariConfig.setJdbcUrl(config.getString("ctx.db_url"))
      hikariConfig.setUsername(config.getString("ctx.db_user"))
      hikariConfig.setPassword(config.getString("ctx.db_pass"))
      hikariConfig
    }
    xa <- HikariTransactor.fromHikariConfig[IO](hikariConfig, connectEC)
  yield xa
