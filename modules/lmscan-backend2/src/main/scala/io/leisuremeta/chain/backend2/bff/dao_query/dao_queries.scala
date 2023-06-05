package io.leisuremeta.chain.lmscan
package backend2
import doobie.*
import doobie.implicits.*
import doobie.util.ExecutionContexts
import io.leisuremeta.chain.lmscan.common.model.dao.Tx
import com.typesafe.config.ConfigFactory
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}

val config = ConfigFactory.load()
val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
  config.getString("ctx.db_className"), // driver classname
  config.getString("ctx.db_url"),       // connect URL (driver-specific)
  config.getString("ctx.db_user"),      // user
  config.getString("ctx.db_pass"),      // password
)

object Queries:

  def getTx =
    sql"select * from tx"
      .query[Tx] // DAO
      .stream
      .take(5)
      .compile // commont option
      .toList
      .transact(xa)
      .attemptSql

//       val xa = Transactor.fromDriverManager[IO](
//   "org.postgresql.Driver",
//   "jdbc:postgresql:database",
//   "user",
//   "password"
// )

// val query: ConnectionIO[List[Tx]] =
//   sql"SELECT * FROM tx".query[Tx].stream.take(5).compile.toList

// val result: EitherT[IO, Throwable, List[Tx]] =
//   sql"SELECT * FROM tx".query[Tx].stream.take(5).compile.toList.attemptSql

// val program: IO[Unit] = result.value.flatMap {
//   case Right(txList) => IO(println(txList))
//   case Left(error)   => IO(println(s"An error occurred: $error"))
// }
// // program.unsafeRunSync()
