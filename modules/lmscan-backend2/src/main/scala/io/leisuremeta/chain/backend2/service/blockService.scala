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

object BlockService:

  // /block?pipe=take(10)
  // /block?pipe=drop(100),take(10)
  // /block?pipe=hash(89ab4d153b6e62269a8bd8a0d3a65689bf1479d3fe5f5ffc4d0baae68ac53c66)

  def getBlockAsync[F[_]: Async](pipeString: Option[String]) =
    transactor
      .use(xa =>
        for blocks <- BlockRepository
            .getBlockPipeAsync(pipeString)
            .transact(xa)
        yield blocks.map(Dao2Dto.block2block_self),
      )
      .pipe(QueriesPipe.genericAsyncQueryPipe)

  def getTxAsync_block_type2[F[_]: Async](pipeString: Option[String]) =
    transactor
      .use(xa =>
        for txs <- TxRepository
            .getTxPipeAsync(pipeString)
            .transact(xa)
        yield txs.map(Dao2Dto.tx2tx_type2),
      )
      .pipe(QueriesPipe.genericAsyncQueryPipe)
