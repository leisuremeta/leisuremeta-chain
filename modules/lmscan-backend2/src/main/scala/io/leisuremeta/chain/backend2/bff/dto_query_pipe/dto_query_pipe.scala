package io.leisuremeta.chain.lmscan
package backend2
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import doobie.util.ExecutionContexts
import io.leisuremeta.chain.lmscan.common.model.dao.Tx
import com.typesafe.config.ConfigFactory
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import io.leisuremeta.chain.lmscan.backend2.CatsUtil.eitherToEitherT
import cats.implicits.*
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.common.model.Dao2Dto
import java.sql.SQLException
import io.leisuremeta.chain.lmscan.common.model.dao.Account
import io.leisuremeta.chain.lmscan.common.model.dto.DTO_Account

object QueriesPipe:

  def pipeTx[F[_]: Async](q: IO[Either[SQLException, List[Tx]]]) =
    q
      .unsafeRunSync()
      .pipe(eitherToEitherT)
      .map(Dao2Dto.tx_type1)

  def pipeAccount[F[_]: Async](q: IO[Either[SQLException, List[Account]]]) =
    q
      .unsafeRunSync()
      .pipe(eitherToEitherT)
      .map(Dao2Dto.account)
