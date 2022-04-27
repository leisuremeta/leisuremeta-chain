package org.leisuremeta.lmchain.core
package node
package repository

import cats.{Functor, Monad}
import cats.data.{EitherT, Kleisli}
import cats.implicits._

import crypto.MerkleTrieNode
import crypto.MerkleTrie.{NodeStore, MerkleTrieState}
import crypto.MerkleTrieNode.{MerkleHash, MerkleRoot}
import failure.DecodingFailure
import model.{Account, NameState, TokenState, Transaction}
import model.Transaction.Token.DefinitionId
import store.KeyValueStore

trait StateRepository[F[_], K, V] {

  def get(
      merkleRoot: MerkleRoot[K, V]
  ): EitherT[F, DecodingFailure, Option[MerkleTrieNode[K, V]]]

  def put(state: MerkleTrieState[K, V]): F[Unit]

}

object StateRepository {

  type Name[F[_]]    = StateRepository[F, Account.Name, NameState]
  type Token[F[_]]   = StateRepository[F, DefinitionId, TokenState]
  type Balance[F[_]] = StateRepository[F, (Account, Transaction.Input.Tx), Unit]

  implicit def nodeStore[F[_]: Functor, K, V](implicit
      sr: StateRepository[F, K, V]
  ): NodeStore[F, K, V] =
    Kleisli(sr.get(_).leftMap(_.msg))

  def fromStores[F[_]: Monad, K, V](implicit
      stateKvStore: KeyValueStore[F, MerkleHash[K, V], MerkleTrieNode[K, V]]
  ): StateRepository[F, K, V] = new StateRepository[F, K, V] {

    def get(
        merkleRoot: MerkleRoot[K, V]
    ): EitherT[F, DecodingFailure, Option[MerkleTrieNode[K, V]]] =
      stateKvStore.get(merkleRoot)

    def put(state: MerkleTrieState[K, V]): F[Unit] = for {
      _ <- Monad[F].pure(scribe.debug(s"Putting state: $state"))
      _ <- state.diff.removal.toList.traverse { stateKvStore.remove(_) }
      _ <- state.diff.addition.toList.traverse { case (k, v) =>
        stateKvStore.put(k, v)
      }
      _ <- Monad[F].pure(scribe.debug(s"Putting completed: $state"))
    } yield ()
  }
}
