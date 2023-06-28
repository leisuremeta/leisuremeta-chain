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

object TxService:

  // /tx?pipe=take(10)
  // /tx?pipe=drop(100),take(10)
  // /tx?pipe=hash(89ab4d153b6e62269a8bd8a0d3a65689bf1479d3fe5f5ffc4d0baae68ac53c66)
  // /tx?pipe=blockHash(dfab606387674d80efe42d38ac31adfeb135689742c2ba8ebd3ca40e1968e71c)
  // /tx?pipe=addr(a7fc7ad709d99f43fe056143aa7e0ae03842d2c5),take(1)
  // /tx?pipe=subtype(TransferFungibleToken),take(10)

  def getTxAsyncGeneric[F[_]: Async](pipeString: Option[String], dto: String) =
    ???

  def getTxAsync[F[_]: Async](pipeString: Option[String]) =
    transactor
      .use(xa =>
        for txs <- TxRepository
            .getTxPipeAsync(pipeString)
            .transact(xa)
        yield txs.map(Dao2Dto.tx2tx_self),
      )
      .pipe(QueriesPipe.genericAsyncQueryPipe)

  def getTxAsync_tx_type2[F[_]: Async](pipeString: Option[String]) =
    transactor
      .use(xa =>
        for txs <- TxRepository
            .getTxPipeAsync(pipeString)
            .transact(xa)
        yield txs.map(Dao2Dto.tx2tx_type2),
      )
      .pipe(QueriesPipe.genericAsyncQueryPipe)
