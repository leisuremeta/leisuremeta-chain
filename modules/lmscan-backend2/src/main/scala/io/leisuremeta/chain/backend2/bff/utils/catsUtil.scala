package io.leisuremeta.chain.lmscan
package backend2

import cats.implicits.*
import cats.data.EitherT
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}

object CatsUtil:
  def genEither[F[_]: Async, T](result: T) =
    EitherT.rightT[F, String](result)
