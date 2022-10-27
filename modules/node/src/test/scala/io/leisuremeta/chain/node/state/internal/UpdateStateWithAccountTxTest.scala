package io.leisuremeta.chain
package node
package state
package internal

import java.time.Instant

import cats.data.EitherT
import cats.effect.IO
import cats.effect.unsafe.implicits.global

import api.model.{
  Account,
  AccountSignature,
  NetworkId,
  PublicKeySummary,
  Signed,
  StateRoot,
  Transaction,
}
import lib.codec.byte.ByteEncoder.ops.*
import lib.crypto.CryptoOps
import lib.crypto.Hash.ops.*
import lib.crypto.Sign.ops.*
import lib.datatype.{BigNat, Utf8}
import lib.merkle.{MerkleTrie, MerkleTrieState}
import lib.failure.DecodingFailure
import repository.StateRepository
import repository.StateRepository.{*, given}

import hedgehog.munit.HedgehogSuite
import hedgehog.*

class UpdateStateWithAccountTxTest extends HedgehogSuite:

  given testKVStore[K, V]: store.KeyValueStore[IO, K, V] =
    new store.KeyValueStore[IO, K, V]:
      private val _map = scala.collection.mutable.Map.empty[K, V]
      def get(key: K): EitherT[IO, DecodingFailure, Option[V]] =
        scribe.info(s"===> test kv store: get($key): current: $_map")
        EitherT.pure[IO, DecodingFailure](_map.get(key))
      def put(key: K, value: V): IO[Unit] =
        scribe.info(s"===> test kv store: put($key, $value): current: $_map")
        IO(_map.put(key, value))
      def remove(key: K): IO[Unit] =
        scribe.info(s"===> test kv store: remove($key): current: $_map")
        IO(_map.remove(key))

  given testStateRepo[K, V]: StateRepository[IO, K, V] =
    StateRepository.fromStores[IO, K, V]

  test("create account") {
    withMunitAssertions { assertions =>

      val keyPair = CryptoOps.fromPrivate(
        BigInt(
          "d0bdcacd4bf48ae4536694d64f1dc47ec33e1c05558c9b11d126ab29df10c86e",
          16,
        ),
      )

      val account = Account(Utf8.unsafeFrom("minter"))

      val pubKeySummary: PublicKeySummary =
        PublicKeySummary.fromPublicKeyHash(keyPair.publicKey.toHash)

      val tx: Transaction = Transaction.AccountTx.CreateAccount(
        networkId = NetworkId(BigNat.unsafeFromLong(1000L)),
        createdAt = Instant.parse("2022-06-20T00:00:00Z"),
        account = account,
        ethAddress = None,
        guardian = None,
      )

      val Right(sig) = keyPair.sign(tx)

      val accountSig = AccountSignature(sig = sig, account = account)

      val signedTx = Signed(sig = accountSig, value = tx)

      val baseState = GossipDomain.MerkleState(
        account = GossipDomain.MerkleState.AccountMerkleState
          .from(StateRoot.AccountStateRoot.empty),
        group = GossipDomain.MerkleState.GroupMerkleState
          .from(StateRoot.GroupStateRoot.empty),
        token = GossipDomain.MerkleState.TokenMerkleState.from(
          StateRoot.TokenStateRoot.empty,
        ),
        reward = GossipDomain.MerkleState.RewardMerkleState.from(
          StateRoot.RewardStateRoot.empty,
        ),
      )

      val Right((state, txResult)) =
        summon[UpdateState[IO, Transaction.AccountTx]](
          baseState,
          accountSig,
          tx.asInstanceOf[Transaction.AccountTx],
        ).value.unsafeRunSync()

      val result = MerkleTrie
        .get[IO, (Account, PublicKeySummary), PublicKeySummary.Info](
          (account, pubKeySummary).toBytes.bits,
        )
        .runA(state.account.keyState)
        .value
        .unsafeRunSync()

      assertions.assert(result.isRight)
    }
  }
