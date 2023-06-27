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
import java.sql.SQLException
import io.leisuremeta.chain.lmscan.common.model.DTO.Account.Detail
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.common.model.Dao2Dto

object AccountService:
  def getAccountDetailSync[F[_]: Async](address: String) =
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

  def getAccountDetailAsync[F[_]: Async](
      address: String,
  ) =
    transactor
      .use(xa =>
        for
          account <- AccountQuery.getAccountAsync.transact(xa)
          txs <- TxQuery.getTxPipeAsync(Some(s"addr($address)")).transact(xa)
        yield DTO.Account.Detail(
          address = account(0).address,
          balance = account(0).balance,
          value = account(0).balance,
          txList = Some(txs.pipe(Dao2Dto.tx_type1)),
        ),
      )
      .pipe(_.attemptSql)
      .unsafeRunSync()
      .pipe(eitherToEitherT)
