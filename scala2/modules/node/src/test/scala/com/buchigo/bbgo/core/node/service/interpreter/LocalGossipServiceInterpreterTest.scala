package org.leisuremeta.lmchain.core
package node
package service
package interpreter

import java.nio.file.Paths
import java.time.Instant

import cats.data.EitherT
import cats.effect.{IO, Resource}
import io.circe.generic.auto._
import io.circe.refined._
import swaydb.serializers.Default.ByteArraySerializer

import GossipDomain._
import codec.byte.ByteCodec
import codec.circe._
import crypto.{CryptoOps, KeyPair}
import crypto.Hash.ops._
import model.{Address, Signed}
import repository.{BlockRepository, StateRepository, TransactionRepository}
import store.interpreter._

class LocalGossipServiceInterpreterTest extends munit.CatsEffectSuite {

  implicit val ec = scala.concurrent.ExecutionContext.global

  implicit val contextShift = IO.contextShift(ec)

  val key0: KeyPair = CryptoOps.fromPrivate(
    BigInt(
      "e125f1036edd14324ef92b194c66c628f93a661de800ab523b79a467fa608ba5",
      16,
    )
  )
  val address0: Address = Address.fromPublicKeyHash(key0.publicKey.toHash)
  val key1: KeyPair = CryptoOps.fromPrivate(
    BigInt(
      "85700f11c730ef89ce8673b31d8e68cf225697c65877da942b3987129d2797ef",
      16,
    )
  )
  val address1: Address = Address.fromPublicKeyHash(key1.publicKey.toHash)

  val params0: GossipParams = GossipParams(
    nodeAddresses = Map(
      0 -> address0,
      1 -> address1,
    ),
    timeWindowMillis = 10000,
    localKeyPair = key0,
  )

  val params1: GossipParams = params0.copy(localKeyPair = key1)

  implicit def storeIndex[K: ByteCodec, V: ByteCodec]
      : StoreIndexSwayInterpreter[K, V] = {

    implicit val b: swaydb.Bag[IO] = swaydb.cats.effect.Bag(contextShift, ec)
    val map: swaydb.Map[K, Array[Byte], Nothing, IO] =
      swaydb.memory.Map[K, Array[Byte], Nothing, IO]().unsafeRunSync()
    new StoreIndexSwayInterpreter[K, V](map, Paths.get("/"))
  }

  implicit val blockHashStore: store.HashStore[IO, model.Block] =
    store.HashStore.fromKeyValueStore[IO, model.Block]

  implicit def stateRepo[K, V] = StateRepository.fromStores[IO, K, V]

  implicit def txRepo: TransactionRepository[IO] =
    TransactionRepository.fromStores[IO]
  implicit def blockRepo: BlockRepository[IO] =
    BlockRepository.fromStores[IO]

  val genesisBlockPutResult =
    blockRepo.put(NodeInitializationService.GenesisBlock).value.unsafeRunSync()
  val genesisBlockGetResult =
    blockHashStore
      .get(NodeInitializationService.GenesisBlock.toHash)
      .value
      .unsafeRunSync()

  val service = ResourceFixture[LocalGossipService[IO]](
    Resource.make[IO, LocalGossipService[IO]] {
      LocalGossipServiceInterpreter
        .build[IO](
          bestConfirmedBlock = NodeInitializationService.GenesisBlock,
          params = params0,
        )
    }(_ => IO.unit)
  )

  val twoService =
    ResourceFixture[(LocalGossipService[IO], LocalGossipService[IO])](
      Resource.make[IO, (LocalGossipService[IO], LocalGossipService[IO])] {
        for {
          service0 <- LocalGossipServiceInterpreter
            .build[IO](
              bestConfirmedBlock = NodeInitializationService.GenesisBlock,
              params = params0,
            )
          service1 <- LocalGossipServiceInterpreter
            .build[IO](
              bestConfirmedBlock = NodeInitializationService.GenesisBlock,
              params = params1,
            )
        } yield (service0, service1)
      }(_ => IO.unit)
    )

  val tx: Signed.Tx = {
    io.circe.parser
      .decode[Signed.Tx]("""{
  "sig" : {
    "NamedSignature" : {
      "name" : "root",
      "sig" : {
        "v" : 28,
        "r" : "9200f784c76056f51edbc473e885565072cbc5f0de386d71058d08fba126ed40",
        "s" : "686b60c447b4bbedcc6fe86cc24928f9b80f86c784853db103f8f6b43563eab"
      }
    }
  },
  "value" : {
    "type" : "CreateName",
    "value" : {
      "networkId" : 10,
      "createdAt" : "2021-11-11T15:48:35.505746Z",
      "name" : "root",
      "state" : {
        "addressess" : {
          "b90ad246fff329280f7c1d535c75b759dd9ba91e" : 1
        },
        "threshold" : 1,
        "guardian" : null
      }
    }
  }
}""").toOption
      .get
  }

  service.test("get") { (service: LocalGossipService[IO]) =>
    val localGossip: GossipDomain.LocalGossip = service.get.unsafeRunSync()

    assertEquals(
      localGossip.bestConfirmed._2,
      NodeInitializationService.GenesisBlock,
    )
  }

  service.test("onNewTx") { (service: LocalGossipService[IO]) =>
    for {
      _           <- service.onNewTx(tx).value
      localGossip <- service.get
    } yield {
      assertEquals(localGossip.newTxs.values.toSet, Set(tx))
    }
  }

  service.test("generateNewBlockSuggestion") {
    (service: LocalGossipService[IO]) =>
      val now = Instant.parse("2021-11-11T15:48:40Z") // 1636645720000
      for {
        _               <- service.onNewTx(tx).value
        blockHashEither <- service.generateNewBlockSuggestion(now).value
        localGossip     <- service.get
      } yield {
        blockHashEither match {
          case Left(msg) => fail(s"fail to get new block hash: $msg")
          case Right(newBlockHash) =>
            val newBlock = localGossip.newBlockSuggestions.get(newBlockHash)
            val blockTxHashes = newBlock.map(_._1.transactionHashes)
            assertEquals(blockTxHashes, Some(Set(tx.toHash)))
        }
      }
  }

  twoService.test("onNewBlockSuggestion") { case (service0, service1) =>
    val now = Instant.parse("2021-11-11T15:48:40Z") // 1636645720000
    val testEitherT = for {
      _           <- service0.onNewTx(tx)
      _           <- service1.onNewTx(tx)
      blockHash   <- service0.generateNewBlockSuggestion(now)
      localGossip <- EitherT.right[String](service0.get)
      (block, _) <- EitherT.fromOption[IO](
        localGossip.newBlockSuggestions.get(blockHash),
        s"block not found: $blockHash",
      )
      _            <- service1.onNewBlockSuggestion(block)
      localGossip1 <- EitherT.right[String](service1.get)
    } yield {
      val (bestConfirmedHash, _) = localGossip1.bestConfirmed
      assertEquals(bestConfirmedHash, blockHash)
    }
    testEitherT.value.map {
      case Left(msg) => fail(msg)
      case Right(_)  => ()
    }
  }

  twoService.test("onNewBlockVote") { case (service0, service1) =>
    val now = Instant.parse("2021-11-11T15:48:40Z") // 1636645720000
    val testEitherT = for {
      _            <- service0.onNewTx(tx)
      _            <- service1.onNewTx(tx)
      blockHash    <- service0.generateNewBlockSuggestion(now)
      localGossip0 <- EitherT.right[String](service0.get)
      (block, _) <- EitherT.fromOption[IO](
        localGossip0.newBlockSuggestions.get(blockHash),
        s"block not found: $blockHash",
      )
      _            <- service1.onNewBlockSuggestion(block)
      localGossip1 <- EitherT.right[String](service1.get)
      sig = (localGossip1.bestConfirmed._2.votes -- block.votes).head
      _              <- service0.onNewBlockVote(blockHash, 1, sig)
      localGossip0_1 <- EitherT.right[String](service0.get)

    } yield {
      val (bestConfirmedHash, _) = localGossip0_1.bestConfirmed
      assertEquals(bestConfirmedHash, blockHash)
    }
    testEitherT.value.map {
      case Left(msg) => fail(msg)
      case Right(_)  => ()
    }
  }
}
