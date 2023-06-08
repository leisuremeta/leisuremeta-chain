package io.leisuremeta.chain.lmscan
package backend2

import java.sql.SQLException
import cats.data.EitherT
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import common.ExploreApi
import cats.implicits.catsSyntaxEitherId
import io.leisuremeta.chain.lmscan.common.model.dto.DTO_Tx

object ErrorHandle:
  def genMsg[F[_]: Async, A, B](pipe: EitherT[F, A, B]) =
    pipe.leftMap { (errMsg) =>
      scribe.error(s"errorMsg: $errMsg")
      (ExploreApi
        .ServerError(s"errorMsg: $errMsg"))
        .asLeft[ExploreApi.UserError]
    }
