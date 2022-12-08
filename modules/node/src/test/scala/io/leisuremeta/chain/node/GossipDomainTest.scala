package io.leisuremeta.chain
package node

import java.time.Instant

import scala.collection.immutable.TreeMap

import cats.Id
import cats.data.EitherT
import cats.effect.IO
import cats.effect.unsafe.implicits.global

import io.circe.generic.auto.given
import io.circe.refined.given

import GossipDomain.*
import api.model.{
  Account,
  AccountSignature,
  NetworkId,
  PublicKeySummary,
  Block,
  Signed,
  Transaction,
  TransactionWithResult,
}
import lib.crypto.{CryptoOps, KeyPair}
import lib.crypto.Hash.ops.*
import lib.crypto.Sign.ops.*
import lib.datatype.{BigNat, Utf8}
import lib.failure.DecodingFailure
import repository.{BlockRepository, StateRepository, TransactionRepository}
import service.StateService
import store.{KeyValueStore, SingleValueStore, StoreIndex}

import hedgehog.munit.HedgehogSuite
import hedgehog.*

class GossipDomainTest extends HedgehogSuite:

  val initial: LocalGossip =
    LocalGossip.empty(
      service.NodeInitializationService.genesisBlock(
        Instant.parse("2020-05-20T09:00:00.00Z"),
      ),
    )
  val key0: KeyPair = CryptoOps.fromPrivate(
    BigInt(
      "e125f1036edd14324ef92b194c66c628f93a661de800ab523b79a467fa608ba5",
      16,
    ),
  )
  val address0: PublicKeySummary =
    PublicKeySummary.fromPublicKeyHash(key0.publicKey.toHash)
  val key1: KeyPair = CryptoOps.fromPrivate(
    BigInt(
      "85700f11c730ef89ce8673b31d8e68cf225697c65877da942b3987129d2797ef",
      16,
    ),
  )
  val address1: PublicKeySummary =
    PublicKeySummary.fromPublicKeyHash(key1.publicKey.toHash)
  val params0: GossipParams = GossipParams(
    nodeAddresses = Map(
      0 -> address0,
      1 -> address1,
    ),
    timeWindowMillis = 10000,
    localKeyPair = key0,
  )
  val params1: GossipParams = params0.copy(localKeyPair = key1)
  val account               = Account(Utf8.unsafeFrom("alice"))
  val txRaw: Transaction = Transaction.AccountTx.CreateAccount(
    networkId = NetworkId(BigNat.unsafeFromLong(1000L)),
    createdAt = Instant.parse("2020-05-22T09:00:00.00Z"),
    account = account,
    ethAddress = None,
    guardian = None,
  )
  val txHash = txRaw.toHash
  val keyPair = CryptoOps.fromPrivate(
    BigInt(
      "f7f0bad6ea0f32173c539a3d38913fd4b221a8a4d709197f2f83a05e62f9f602",
      16,
    ),
  )
  val Right(sig)    = keyPair.sign(txRaw)
  val accountSig    = AccountSignature(sig, account)
  val tx: Signed.Tx = Signed(accountSig, txRaw)
  given updateState: UpdateState[IO] = new UpdateState[IO]:
    given storeIndex[K: Ordering, V]: StoreIndex[IO, K, V] =
      new StoreIndex[IO, K, V]:
        private var store: TreeMap[K, V] = TreeMap.empty
        override def get(key: K): EitherT[IO, DecodingFailure, Option[V]] =
          EitherT.rightT[IO, DecodingFailure](store.get(key))
        override def put(key: K, value: V): IO[Unit] =
          IO.pure(store += (key -> value))
        override def remove(key: K): IO[Unit] = IO.pure{
          this.store = store.removed(key)
        }
        override def from(
            key: K,
            offset: Int,
            limit: Int,
        ): EitherT[IO, DecodingFailure, List[(K, V)]] =
          EitherT.pure(store.iteratorFrom(key).drop(offset).take(limit).toList)

    given kvStore[K, V]: KeyValueStore[IO, K, V] =
      new KeyValueStore[IO, K, V]:
        private var store: Map[K, V] = Map.empty
        override def get(key: K): EitherT[IO, DecodingFailure, Option[V]] =
          EitherT.rightT[IO, DecodingFailure](store.get(key))
        override def put(key: K, value: V): IO[Unit] =
          IO.pure(store += (key -> value))
        override def remove(key: K): IO[Unit] = IO.pure(store -= key)

    given sStore[A]: SingleValueStore[IO, A] =
      SingleValueStore.fromKeyValueStore[IO, A]
    given stateRepo[K, V]: StateRepository[IO, K, V] =
      StateRepository.fromStores[IO, K, V]
    given blockRepo: BlockRepository[IO] =
      BlockRepository.fromStores[IO]
    given txRepo: TransactionRepository[IO] =
      TransactionRepository.fromStores[IO]
    override def apply(
        state: MerkleState,
        tx: Signed.Tx,
    ): EitherT[IO, String, (MerkleState, TransactionWithResult)] =
      StateService.updateStateWithTx[IO](state, tx)

  test("onNewTx") {
    withMunitAssertions { assertions =>

      val Right(gossip1) = onNewTx[IO](initial, tx).value.unsafeRunSync()

      assertions.assert(gossip1.newTxs.contains(tx.toHash))
    }
  }

  test("generateNewBlockSuggestion") {
    withMunitAssertions { assertions =>
      val Right(gossip1) = onNewTx[IO](initial, tx).value.unsafeRunSync()
      val now = Instant.parse("2021-11-11T15:48:40Z") // 1636645720000

      val Right((_, blockSuggestion)) =
        generateNewBlockSuggestion[IO](gossip1, now, params0).value
          .unsafeRunSync()

      assertions.assertEquals(blockSuggestion.transactionHashes, Set(tx.toHash))
    }
  }
/*
  test("onNewBlockSuggestion - self signed") {
    val Right(gossip1) = onNewTx[Id](initial, tx).value
    val now            = Instant.parse("2021-11-11T15:48:40Z") // 1636645720000
    val Right((blockHash, blockSuggestion)) =
      generateNewBlockSuggestion[Id](gossip1, now, params0).value

    val Right((gossip2, sigOption)) =
      onNewBlockSuggestion[Id](gossip1, blockSuggestion, params0).value

    Result.all(
      List(
        gossip2.newBlockSuggestions.contains(blockSuggestion.toHash) ==== true,
        gossip2.bestBlock.map(_._1) ==== Some(blockHash),
        sigOption ==== None,
      ),
    )
  }

  test("onNewBlockSuggestion - peer signed") {
    val Right(gossip1) = onNewTx[Id](initial, tx).value
    val now            = Instant.parse("2021-11-11T15:48:40Z") // 1636645720000
    val Right((_, blockSuggestion)) =
      generateNewBlockSuggestion[Id](gossip1, now, params0).value

    val Right((gossip2, sigOption)) =
      onNewBlockSuggestion[Id](gossip1, blockSuggestion, params1).value

    Result.all(
      List(
        gossip2.newBlockSuggestions.contains(blockSuggestion.toHash) ==== true,
        sigOption.nonEmpty ==== true,
      ),
    )
  }

  test("onNewBlockSuggestion - wrong time window") {
    val Right(gossip1) = onNewTx[Id](initial, tx).value
    val now            = Instant.parse("2021-11-11T15:48:50Z") // 1636645730000
    val Right((_, blockSuggestion)) =
      generateNewBlockSuggestion[Id](gossip1, now, params0).value

    val blockEither =
      onNewBlockSuggestion[Id](gossip1, blockSuggestion, params0).value

    Result.assert(blockEither.isLeft)
  }

  test("onNewBlockVote - peer vote") {
    val Right(gossip1) = onNewTx[Id](initial, tx).value
    val now            = Instant.parse("2021-11-11T15:48:40Z") // 1636645720000
    val Right((blockHash, blockSuggestion)) =
      generateNewBlockSuggestion[Id](gossip1, now, params0).value
    val Right((gossip2, Some(sig1))) =
      onNewBlockSuggestion[Id](gossip1, blockSuggestion, params1).value

    val ans = onNewBlockVote(
      gossip = gossip2,
      blockHash = blockHash,
      nodeNo = params1.localNodeIndex,
      sig = sig1,
      params = params0,
    )

    ans match
      case Right(gossip3) =>
        gossip3.newBlockVotes ==== Map(
          (blockHash, params1.localNodeIndex) -> sig1,
        )
      case Left(msg) =>
        Result.failure
  }

  test("onNewBlockVote - peer suggest, self vote") {
    val Right(gossip1) = onNewTx[Id](initial, tx).value
    val now            = Instant.parse("2021-11-11T15:48:50Z") // 1636645730000
    val Right((blockHash, blockSuggestion)) =
      generateNewBlockSuggestion[Id](gossip1, now, params1).value
    val Right((gossip2, Some(sig0))) =
      onNewBlockSuggestion[Id](gossip1, blockSuggestion, params0).value

    val ans = onNewBlockVote(
      gossip = gossip2,
      blockHash = blockHash,
      nodeNo = params0.localNodeIndex,
      sig = sig0,
      params = params0,
    )

    ans match
      case Right(gossip3) =>
        Result.all(
          List(
            gossip3.newBlockVotes ==== Map(
              (blockHash, params0.localNodeIndex) -> sig0,
            ),
            gossip3.bestBlock.map(_._1) ==== Some(blockHash),
          ),
        )
      case Left(msg) =>
        Result.failure
  }

  test("onNewBlockVote - suggestor's vote") {
    val Right(gossip1) = onNewTx[Id](initial, tx).value
    val now            = Instant.parse("2021-11-11T15:48:40Z") // 1636645720000
    val Right((blockHash, blockSuggestion)) =
      generateNewBlockSuggestion[Id](gossip1, now, params0).value
    val sig0 = blockSuggestion.votes.head
    val Right((gossip2, _)) =
      onNewBlockSuggestion[Id](gossip1, blockSuggestion, params0).value

    val ans = onNewBlockVote(
      gossip = gossip2,
      blockHash = blockHash,
      nodeNo = params0.localNodeIndex,
      sig = sig0,
      params = params0,
    )
    Result.assert(ans.isLeft)
  }

  test("tryFinalizeBlockWithBlockHash") {
    // initial state with one tx
    val Right(gossip0) = onNewTx[Id](initial, tx).value
    // generate block suggestion from node 0
    val now = Instant.parse("2021-11-11T15:48:40Z") // 1636645720000
    val Right((blockHash, blockSuggestion)) =
      generateNewBlockSuggestion[Id](gossip0, now, params0).value

    // put block suggestion into node 0
    val Right((gossip0_1, None)) =
      onNewBlockSuggestion[Id](gossip0, blockSuggestion, params0).value

    // put block suggestion into node 1
    val Right((gossip1_1 @ _, Some(sig1))) =
      onNewBlockSuggestion[Id](gossip0, blockSuggestion, params1).value

    // put block vote into node 0
    val Right(gossip0_2) = onNewBlockVote(
      gossip = gossip0_1,
      blockHash = blockHash,
      nodeNo = params1.localNodeIndex,
      sig = sig1,
      params = params0,
    )

    // try to finalize block in node 0
    val ans: Either[String, (LocalGossip, List[(Block.BlockHash, Block)])] =
      tryFinalizeBlockWithBlockHash(
        gossip = gossip0_2,
        blockHash = blockHash,
        params = params0,
      )

    ans match
      case Right((gossip0_3, List((blockHash @ _, block)))) =>
        Result.all(
          List(
            gossip0_3.newBlockVotes.keySet ==== Set((blockHash, 1)),
            block.transactionHashes ==== Set(tx.toHash),
            block.votes ==== blockSuggestion.votes + sig1,
          ),
        )
      case _ =>
        Result.failure
  }

  test("tryFinalizeBlockWithBlockHash - unfinalizable case") {
    val Right(gossip1) = onNewTx[Id](initial, tx).value
    val now            = Instant.parse("2021-11-11T15:48:40Z") // 1636645720000
    val Right((blockHash, blockSuggestion)) =
      generateNewBlockSuggestion[Id](gossip1, now, params0).value
    val Right((gossip2, _)) =
      onNewBlockSuggestion[Id](gossip1, blockSuggestion, params0).value

    val ans: Either[String, (LocalGossip, List[(Block.BlockHash, Block)])] =
      tryFinalizeBlockWithBlockHash(
        gossip = gossip2,
        blockHash = blockHash,
        params = params0,
      )

    ans ==== Left("not enough number of vote to finalize: currently 0")
  }

  test("tryFinalizeBlock - multi block finalization") {
    val Right(gossip1) = onNewTx[Id](initial, tx).value
    val instant0       = Instant.parse("2021-11-11T15:48:40Z") // 1636645720000
    val Right((blockHash, blockSuggestion)) =
      generateNewBlockSuggestion[Id](gossip1, instant0, params0).value
    val Right((gossip2, _)) =
      onNewBlockSuggestion[Id](gossip1, blockSuggestion, params0).value
    val Right(gossip3) = onNewTx[Id](gossip2, tx2).value
    val instant1       = Instant.parse("2021-11-12T11:46:40Z") // 1636645730000
    val Right((blockHash2, blockSuggestion2)) =
      generateNewBlockSuggestion[Id](gossip3, instant1, params0).value
    val Right((gossip4, _)) =
      onNewBlockSuggestion[Id](gossip3, blockSuggestion2, params0).value

    val Right((gossip1_1, Some(sig1_1 @ _))) =
      onNewBlockSuggestion[Id](gossip1, blockSuggestion, params1).value
    val Right(gossip1_2) = onNewTx[Id](gossip1_1, tx2).value
    val Right((gossip1_3 @ _, Some(sig1_2))) =
      onNewBlockSuggestion[Id](gossip1_2, blockSuggestion2, params1).value

    val Right(gossip5) =
      onNewBlockVote(
        gossip4,
        blockHash2,
        params1.localNodeIndex,
        sig1_2,
        params0,
      )

    val ans: Either[String, (LocalGossip, List[(Block.BlockHash, Block)])] =
      tryFinalizeBlockWithBlockHash(
        gossip = gossip5,
        blockHash = blockHash2,
        params = params0,
      )

    ans match
      case Left(msg) =>
        Result.failure
      case Right((gossip6 @ _, blocks)) =>
        blocks.unzip._1 ==== List(blockHash, blockHash2)
  }

  test(
    "tryFinalizeBlock - remaining block suggestion states expect to be based on new confirmed state",
  ) {

    val Right(gossip1) = onNewTx[Id](initial, tx).value
    val instant0       = Instant.parse("2021-11-11T15:48:40Z") // 1636645720000
    val Right((blockHash, blockSuggestion)) =
      generateNewBlockSuggestion[Id](gossip1, instant0, params0).value
    val Right((gossip2, _)) =
      onNewBlockSuggestion[Id](gossip1, blockSuggestion, params0).value
    val Right(gossip3) = onNewTx[Id](gossip2, tx2).value
    val instant1       = Instant.parse("2021-11-12T11:46:40Z") // 1636645730000
    val Right((blockHash2, blockSuggestion2)) =
      generateNewBlockSuggestion[Id](gossip3, instant1, params0).value
    val Right((gossip4, _)) =
      onNewBlockSuggestion[Id](gossip3, blockSuggestion2, params0).value

    val Right((gossip1_1 @ _, Some(sig1_1))) =
      onNewBlockSuggestion[Id](gossip1, blockSuggestion, params1).value

    val Right(gossip5) =
      onNewBlockVote(
        gossip4,
        blockHash,
        params1.localNodeIndex,
        sig1_1,
        params0,
      )

    val ans: Either[String, (LocalGossip, List[(Block.BlockHash, Block)])] =
      tryFinalizeBlockWithBlockHash(
        gossip = gossip5,
        blockHash = blockHash,
        params = params0,
      )

    ans match
      case Left(msg) =>
        Result.failure
      case Right((gossip6 @ _, blocks)) =>
        Result.all(
          List(
            blocks.unzip._1 ==== List(blockHash),
            gossip6.newBlockSuggestions(blockHash2)._2.namesState.base ====
              gossip2.newBlockSuggestions(blockHash)._2.namesState.root,
//            gossip6.newBlockSuggestions(blockHash2)._2.tokenState.base ====
//              gossip2.newBlockSuggestions(blockHash)._2.tokenState.root,
//            gossip6.newBlockSuggestions(blockHash2)._2.balanceState.base ====
//              gossip2.newBlockSuggestions(blockHash)._2.balanceState.root,
          ),
        )
  }
 */
