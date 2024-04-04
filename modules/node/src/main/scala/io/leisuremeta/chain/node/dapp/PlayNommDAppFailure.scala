package io.leisuremeta.chain.node.dapp

import cats.{~>, Functor}
import cats.arrow.FunctionK
import cats.data.EitherT
import cats.syntax.bifunctor.*

sealed trait PlayNommDAppFailure:
  def msg: String
object PlayNommDAppFailure:
  final case class External(msg: String) extends PlayNommDAppFailure
  final case class Internal(msg: String) extends PlayNommDAppFailure

  def external(msg: String): PlayNommDAppFailure = External(msg)
  def internal(msg: String): PlayNommDAppFailure = Internal(msg)

  def mapExternal[F[_]: Functor](
      msg: String,
  ): EitherT[F, String, *] ~> EitherT[F, PlayNommDAppFailure, *] =
    FunctionK.lift {
      [A] =>
        (stringOr: EitherT[F, String, A]) =>
          stringOr.leftMap(e => PlayNommDAppFailure.external(s"$msg: $e"))
    }

  def mapInternal[F[_]: Functor](
      msg: String,
  ): EitherT[F, String, *] ~> EitherT[F, PlayNommDAppFailure, *] =
    FunctionK.lift {
      [A] =>
        (stringOr: EitherT[F, String, A]) =>
          stringOr.leftMap(e => PlayNommDAppFailure.internal(s"$msg: $e"))
    }
