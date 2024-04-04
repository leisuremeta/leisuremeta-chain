package io.leisuremeta.chain
package node
package repository

import cats.{Functor, Monad}
import cats.data.{EitherT, Kleisli}
import cats.syntax.traverse.*

import lib.merkle.{MerkleTrie, MerkleTrieNode, MerkleTrieState}
import lib.merkle.MerkleTrieNode.{MerkleHash, MerkleRoot}
import lib.failure.DecodingFailure
import store.KeyValueStore

trait StateRepository[F[_]]:
  def get(merkleRoot: MerkleRoot): EitherT[F, DecodingFailure, Option[MerkleTrieNode]]
  def put(state: MerkleTrieState): EitherT[F, DecodingFailure, Unit]

object StateRepository:
  def apply[F[_]: StateRepository]: StateRepository[F] = summon

  given nodeStore[F[_]: Functor: StateRepository]: MerkleTrie.NodeStore[F] =
    Kleisli(StateRepository[F].get(_).leftMap(_.msg))

  given fromStores[F[_]: Monad](using
    stateKvStore: KeyValueStore[F, MerkleHash, MerkleTrieNode],
  ): StateRepository[F] with
    def get(merkleRoot: MerkleRoot): EitherT[F, DecodingFailure, Option[MerkleTrieNode]] =
      stateKvStore.get(merkleRoot)

    def put(state: MerkleTrieState): EitherT[F, DecodingFailure, Unit] = for
//      _ <- EitherT.pure(scribe.info(s"Putting state: $state"))
      _ <- state.diff.toList.traverse { case (hash, (node, count)) =>
        if count <= 0 then EitherT.pure(()) else stateKvStore.get(hash).flatMap{
          case None => EitherT.right(stateKvStore.put(hash, node))
          case _ => EitherT.pure(())
        }
      }
//      _ <- EitherT.pure(scribe.info(s"Putting completed: $state"))
    yield ()
