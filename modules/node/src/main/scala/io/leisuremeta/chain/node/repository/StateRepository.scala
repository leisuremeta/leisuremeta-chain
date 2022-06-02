package io.leisuremeta.chain
package node
package repository

import cats.{Functor, Monad}
import cats.data.{EitherT, Kleisli, OptionT}
import cats.implicits.*

import api.model.{Account, GroupId, GroupData, PublicKeySummary}
import lib.datatype.BigNat
import lib.merkle.{MerkleTrie, MerkleTrieNode, MerkleTrieState}
import lib.merkle.MerkleTrie.NodeStore
import lib.merkle.MerkleTrieNode.{MerkleHash, MerkleRoot}
import lib.failure.DecodingFailure
import store.KeyValueStore

trait StateRepository[F[_], K, V]:
  def get(
      merkleRoot: MerkleRoot[K, V],
  ): EitherT[F, DecodingFailure, Option[MerkleTrieNode[K, V]]]

  def put(state: MerkleTrieState[K, V]): F[Unit]

object StateRepository:
  object AccountState:
    type Name[F[_]] = StateRepository[F, Account, Option[Account]]
    type Key[F[_]] = StateRepository[F, (Account, PublicKeySummary), PublicKeySummary.Info]

  object GroupState:
    type Group[F[_]] = StateRepository[F, GroupId, GroupData]
    type GroupAccount[F[_]] = StateRepository[F, (GroupId, Account), Unit]

  given nodeStore[F[_]: Functor, K, V](using
      sr: StateRepository[F, K, V],
  ): NodeStore[F, K, V] = Kleisli(sr.get(_).leftMap(_.msg))

  def fromStores[F[_]: Monad, K, V](using
      stateKvStore: KeyValueStore[
        F,
        MerkleHash[K, V],
        (MerkleTrieNode[K, V], BigNat),
      ],
  ): StateRepository[F, K, V] = new StateRepository[F, K, V]:

    def get(
        merkleRoot: MerkleRoot[K, V],
    ): EitherT[F, DecodingFailure, Option[MerkleTrieNode[K, V]]] =
      OptionT(stateKvStore.get(merkleRoot)).map(_._1).value

    def put(state: MerkleTrieState[K, V]): F[Unit] = for
      _ <- Monad[F].pure(scribe.debug(s"Putting state: $state"))
      _ <- state.diff.toList.traverse { case (hash, (node, count)) =>
        stateKvStore.get(hash).value.flatMap {
          case Right(Some((node0, count0))) =>
            val count1 = count0.toBigInt.toLong + count
            if count1 <= 0 then stateKvStore.remove(hash)
            else if count > 0 then
              stateKvStore.put(hash, (node, BigNat.unsafeFromLong(count1)))
            else Monad[F].unit
          case Right(None) =>
            if count > 0 then
              stateKvStore.put(hash, (node, BigNat.unsafeFromLong(count)))
            else Monad[F].unit
          case Left(err) => throw new Exception(s"Error: $err")
        }
      }
      _ <- Monad[F].pure(scribe.debug(s"Putting completed: $state"))
    yield ()
