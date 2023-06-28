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

object TxService:

  // /tx?pipe=take(3),absend,asd,asd,asd&dto=txDetailpage&view=form
  // /tx?pipe=take(10)
  // /tx?pipe=drop(10*10),take(10) -- 10번째 페이지

  def getTxAsync[F[_]: Async](pipeString: Option[String]) =
    transactor
      .use(xa =>
        for txs <- TxRepository
            .getTxPipeAsync(pipeString)
            .transact(xa)
        yield txs.map(Dao2Dto.tx2tx_self),
      )
      .pipe(QueriesPipe.genericAsyncQueryPipe)
