package io.leisuremeta.chain.lmscan
package backend2
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import doobie.util.ExecutionContexts
import com.typesafe.config.ConfigFactory
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import io.leisuremeta.chain.lmscan.backend2.CatsUtil.eitherToEitherT
import cats.implicits.*
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.common.model.Dao2Dto
import java.sql.SQLException
import io.leisuremeta.chain.lmscan.common.model.*

object QueriesPipe:
  def genericQueryPipe[F[_]: Async, A, B](
      f: A => B,
  )(q: IO[Either[SQLException, A]]) =
    q
      .unsafeRunSync()
      .pipe(eitherToEitherT)
      .map(f)

  def genericAsyncQueryPipe[F[_]: Async, T](q: IO[T]) =
    q
      .pipe(_.attemptSql)
      .unsafeRunSync()
      .pipe(eitherToEitherT)

  def pipeTx[F[_]: Async](q: IO[Either[SQLException, List[DAO.Tx]]]) =
    q
      .pipe(genericQueryPipe(Dao2Dto.tx_type1))

  def pipeTxCount[F[_]: Async](q: IO[Either[SQLException, Int]]) =
    q
      .pipe(genericQueryPipe(d => new DTO.Tx.count(count = d)))

  def pipeAccount[F[_]: Async](q: IO[Either[SQLException, List[DAO.Account]]]) =
    q
      .pipe(genericQueryPipe(Dao2Dto.account))

  def pipeSummary[F[_]: Async](q: IO[Either[SQLException, List[DAO.Summary]]]) =
    q
      .pipe(
        genericQueryPipe(d =>
          new DTO.Summary.SummaryMain(
            Some(d(0).id),
            Some(d(0).lmPrice),
            Some(d(0).blockNumber),
            Some(d(0).totalAccounts),
            Some(d(0).createdAt),
            Some(d(0).totalTxSize),
            Some(d(0).total_balance),
          ),
        ),
      )
