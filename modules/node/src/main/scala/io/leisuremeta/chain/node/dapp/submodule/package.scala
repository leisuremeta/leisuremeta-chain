package io.leisuremeta.chain
package node
package dapp
package submodule

import cats.Monad
import cats.data.{EitherT, StateT}
import lib.merkle.MerkleTrieState

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

def checkInternal[F[_]: Monad](
    test: Boolean,
    errorMessage: String,
): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, Unit] =
  StateT.liftF:
    EitherT.cond(
      test,
      (),
      PlayNommDAppFailure.internal(errorMessage),
    )

def fromOption[F[_]: Monad, A](
    option: Option[A],
    errorMessage: String,
): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, A] =
  StateT.liftF {
    EitherT.fromOption(option, PlayNommDAppFailure.external(errorMessage))
  }

def fromEitherExternal[F[_]: Monad, A](
  either: Either[String, A]
): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, A] =
  StateT.liftF {
    EitherT.fromEither(either).leftMap(PlayNommDAppFailure.external)
  }

def fromEitherInternal[F[_]: Monad, A](
  either: Either[String, A]
): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, A] =
  StateT.liftF {
    EitherT.fromEither(either).leftMap(PlayNommDAppFailure.internal)
  }

def pure[F[_]: Monad, A](
    a: A,
): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, A] =
  StateT.liftF(EitherT.pure(a))

def unit[F[_]: Monad] = pure[F, Unit](())
