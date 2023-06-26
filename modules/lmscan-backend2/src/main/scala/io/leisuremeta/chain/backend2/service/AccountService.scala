package io.leisuremeta.chain.lmscan
package backend2

import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import io.leisuremeta.chain.lmscan.backend2.AccountQuery
import io.leisuremeta.chain.lmscan.backend2.QueriesPipe
import util.chaining.scalaUtilChainingOps
import io.leisuremeta.chain.lmscan.backend2.TxQuery
import io.leisuremeta.chain.lmscan.common.model.DTO
import doobie.implicits.*
import scala.util.chaining.*
import cats.effect.unsafe.implicits.global
import io.leisuremeta.chain.lmscan.backend2.CatsUtil.eitherToEitherT
import scala.concurrent.ExecutionContext
import cats.implicits.toFlatMapOps

object AccountService:
  def getAccountDetail[F[_]: Async](address: String) =
    val r = for
      account <-
        AccountQuery.getAccount
          .pipe(QueriesPipe.pipeAccount[F])
      txList <-
        TxQuery
          .getTxPipe(Some(s"addr($address)"))
          .pipe(QueriesPipe.pipeTx[F])
    yield (account, txList)
    r.map { (account, txList) =>
      DTO.Account.Detail(
        address = account.address,
        balance = account.balance,
        value = account.balance,
        txList = Some(txList),
      )
    }
//   def getAccountDetailAsync[F[_]: Async](address: String)(implicit
//       ec: ExecutionContext,
//   ) =
//     val combinedQuery = for
//       account <-
//         AccountQuery.getAccount
//           .unsafeRunAsync(
//             {
//               case Left(error)   => println(s"Error: $error")
//               case Right(result) => println(s"Result: $result")
//             },
//           )
//       //   .getClass()
//       //   .pipe(eitherToEitherT)
//       txList <-
//         TxQuery
//           .getTxPipe(Some(s"addr($address)"))
//           .unsafeRunSync()
//           .pipe(eitherToEitherT)
//     yield (account, txList)

// combinedQuery.unsafeRunAsync()
// r.map { (account, txList) =>
//   DTO.Account.Detail(
//     address = account.address,
//     balance = account.balance,
//     value = account.balance,
//     txList = Some(txList),
//   )
// }

// doobie join ..
//     import doobie._
// import doobie.implicits._
// import doobie.postgres._
// import doobie.postgres.implicits._

// def joinOperation(): IO[List[(String, String)]] = {
//   val query = for {
//     movie <- sql"SELECT id, title FROM movies".query[(String, String)]
//     actor <- sql"SELECT id, name FROM actors".query[(String, String)]
//     if movie._1 == actor._1
//   } yield (movie._2, actor._2)

//   query.transact(xa)
// }
