package io.leisuremeta.chain.lmscan
package backend2

import cats.implicits.*
import cats.data.EitherT
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}

object CatsUtil:
  def genEither[F[_]: Async, T](result: T) =
    EitherT.rightT[F, String](result)

  def eitherToEitherT[F[_]: Async, A, B](
      either: Either[A, B],
  ): EitherT[F, A, B] =
    EitherT.fromEither[F](either)
