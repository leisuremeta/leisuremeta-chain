package io.leisuremeta.chain
package node
package state
package internal

import java.time.Instant

import cats.data.{EitherT, StateT}
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
  TransactionWithResult,
}
import api.model.account.EthAddress
import lib.codec.byte.ByteEncoder.ops.*
import lib.crypto.CryptoOps
import lib.crypto.Hash.ops.*
import lib.crypto.Sign.ops.*
import lib.datatype.{BigNat, Utf8}
import lib.merkle.{MerkleTrie, GenericMerkleTrieState}
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

      val Right(sig) = keyPair.sign(tx): @unchecked

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
        ).value.unsafeRunSync(): @unchecked

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

  test("update ethAddress after UpdateAccount") {

    withMunitAssertions { assertions =>
      val aliceKey = CryptoOps.fromPrivate(
        BigInt(
          "d0bdcacd4bf48ae4536694d64f1dc47ec33e1c05558c9b11d126ab29df10c86e",
          16,
        ),
      )

      val bobKey = CryptoOps.fromPrivate(
        BigInt(
          "56599e60c7a6c5bf818dbd1734187049a949f3e4cd37a84fa6364705347c9ee6",
          16,
        ),
      )

      val alice = Account(Utf8.unsafeFrom("alice"))
      val bob   = Account(Utf8.unsafeFrom("bob"))

      def signAlice(tx: Transaction): Signed.Tx =
        val Right(sig) = aliceKey.sign(tx): @unchecked
        val accountSig = AccountSignature(sig = sig, account = alice)
        Signed(sig = accountSig, value = tx)

      def signBob(tx: Transaction): Signed.Tx =
        val Right(sig) = bobKey.sign(tx): @unchecked
        val accountSig = AccountSignature(sig = sig, account = bob)
        Signed(sig = accountSig, value = tx)

      val ethAddress: EthAddress = EthAddress {
        Utf8.unsafeFrom("0xefD277f6da7ac53e709392044AE98220Df142753")
      }

      val createAliceTx: Transaction = Transaction.AccountTx.CreateAccount(
        networkId = NetworkId(BigNat.unsafeFromLong(1000L)),
        createdAt = Instant.parse("2022-06-20T00:00:00Z"),
        account = alice,
        ethAddress = Some(ethAddress),
        guardian = None,
      )

      val createBobTx: Transaction = Transaction.AccountTx.CreateAccount(
        networkId = NetworkId(BigNat.unsafeFromLong(1000L)),
        createdAt = Instant.parse("2022-06-20T00:00:01Z"),
        account = bob,
        ethAddress = Some(ethAddress),
        guardian = None,
      )

      val updateAliceTx: Transaction = Transaction.AccountTx.UpdateAccount(
        networkId = NetworkId(BigNat.unsafeFromLong(1000L)),
        createdAt = Instant.parse("2022-06-20T00:00:02Z"),
        account = alice,
        ethAddress = Some(ethAddress),
        guardian = None,
      )

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

      def update(signedTx: Signed.Tx): StateT[
        EitherT[IO, String, *],
        GossipDomain.MerkleState,
        TransactionWithResult,
      ] = StateT { (ms: GossipDomain.MerkleState) =>
        summon[UpdateState[IO, Transaction.AccountTx]](
          ms,
          signedTx.sig,
          signedTx.value.asInstanceOf[Transaction.AccountTx],
        )
      }

      def getAccountOfEthAddress(
          ethAddress: EthAddress,
      ): StateT[EitherT[IO, String, *], GossipDomain.MerkleState, Option[
        Account,
      ]] = StateT.inspectF { (ms: GossipDomain.MerkleState) =>
        MerkleTrie
          .get[IO, EthAddress, Account](ethAddress.toBytes.bits)
          .runA(ms.account.ethState)
      }

      val testProgram = for
        _       <- update(signAlice(createAliceTx))
        _       <- update(signBob(createBobTx))
        _       <- update(signAlice(updateAliceTx))
        account <- getAccountOfEthAddress(ethAddress)
      yield account

      val result = testProgram.runA(baseState).value.unsafeRunSync()
      
      assertions.assertEquals(result, Right(Some(alice)))
    }
  }
