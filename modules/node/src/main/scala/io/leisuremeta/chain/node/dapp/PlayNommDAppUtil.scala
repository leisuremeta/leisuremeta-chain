package io.leisuremeta.chain
package node.dapp

import cats.{~>, Monad}
import cats.arrow.FunctionK
import cats.data.{EitherT, StateT}

import lib.merkle.MerkleTrieState


object PlayNommDAppUtil:
  def checkExternal[F[_]: Monad](
      test: Boolean,
      errorMessage: String,
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, Unit] =
    StateT.liftF {
      EitherT.cond(
        test,
        (),
        PlayNommDAppFailure.external(errorMessage),
      )
    }

  def fromOption[F[_]: Monad, A](
      option: Option[A],
      errorMessage: String,
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, A] =
    StateT.liftF {
      EitherT.fromOption(option, PlayNommDAppFailure.external(errorMessage))
    }

  def pure[F[_]: Monad, A](
      a: A,
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, A] =
    StateT.liftF(EitherT.pure(a))

  def unit[F[_]: Monad] = pure[F, Unit](())
