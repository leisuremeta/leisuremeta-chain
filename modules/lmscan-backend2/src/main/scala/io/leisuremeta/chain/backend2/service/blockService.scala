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

import cats.data.EitherT
import io.leisuremeta.chain.lmscan.common.model.Dao2Dto
// import io.leisuremeta.chain.lmscan.common.model.BlockDetail_withTx
import io.leisuremeta.chain.lmscan.common.model.DTO.Block.BlockDetail_withTx

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

  def getBlockAsync_blockDetail_withTx[F[_]: Async](
      hash: String,
  ) =
    transactor
      .use(xa =>
        for blocks <- BlockRepository
            .getBlockPipeAsync(Some(s"hash($hash)"))
            .transact(xa)
          // txs <- TxRepository
          //   .getTxPipeAsync(Some(s"blockHash($hash)"))
          //   .transact(xa)
        yield BlockDetail_withTx(
          number = Some(blocks(0).number),
          hash = Some(blocks(0).hash),
          parentHash = Some(blocks(0).parentHash),
          txCount = Some(blocks(0).txCount),
          eventTime = Some(blocks(0).eventTime),
          createdAt = Some(blocks(0).createdAt),
          // txList = Some(txs.map(Dao2Dto.tx2tx_self)),
        ),
      )
      .pipe(QueriesPipe.genericAsyncQueryPipe)
