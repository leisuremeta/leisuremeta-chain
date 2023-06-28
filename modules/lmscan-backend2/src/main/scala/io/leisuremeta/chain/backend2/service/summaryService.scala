package io.leisuremeta.chain.lmscan
package backend2

import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import io.leisuremeta.chain.lmscan.backend2.AccountRepository
import io.leisuremeta.chain.lmscan.backend2.QueriesPipe
import util.chaining.scalaUtilChainingOps
import io.leisuremeta.chain.lmscan.backend2.TxRepository
import io.leisuremeta.chain.lmscan.common.model.DTO
import doobie.implicits.*
import scala.util.chaining.*
import cats.effect.unsafe.implicits.global
import io.leisuremeta.chain.lmscan.backend2.CatsUtil.eitherToEitherT
import scala.concurrent.ExecutionContext
import cats.implicits.toFlatMapOps
import java.sql.SQLException
import io.leisuremeta.chain.lmscan.common.model.DTO.Account.Detail
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.common.model.Dao2Dto

object SummaryService:

  def getSummaryAsync[F[_]: Async] =
    transactor
      .use(xa =>
        for account <- SummaryRepository.getSummaryAsync.transact(xa)
        yield DTO.Summary.SummaryMain(
          Some(account(0).id),
          Some(account(0).lmPrice),
          Some(account(0).blockNumber),
          Some(account(0).totalAccounts),
          Some(account(0).createdAt),
          Some(account(0).totalTxSize),
          Some(account(0).total_balance),
        ),
      )
      .pipe(QueriesPipe.genericAsyncQueryPipe)
