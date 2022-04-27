package org.leisuremeta.lmchain.core
package node

import java.time.Instant

import cats.Id
import cats.data.EitherT

import io.circe.generic.auto._
import io.circe.refined._

import GossipDomain._
import codec.circe._
import crypto.{CryptoOps, KeyPair}
import crypto.Hash.ops._
import failure.DecodingFailure
import model.{Address, Block, Signed}
import repository.{StateRepository, TransactionRepository}
import service.StateService
import service.StateService.MerkleState
import store.KeyValueStore

class GossipDomainTest extends munit.FunSuite {

  val initial: LocalGossip =
    LocalGossip.empty(service.NodeInitializationService.GenesisBlock)

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

  val tx2: Signed.Tx = {
    io.circe.parser
      .decode[Signed.Tx]("""{
  "sig" : {
    "NamedSignature" : {
      "name" : "root",
      "sig" : {
        "v" : 28,
        "r" : "d91a3c8b506512d9c52c20c7e64cf2bb59b4959ac6d4293d63e199cf262579c9",
        "s" : "5e73107f47f90a8853e43e6bb4a079c5aa33d14499b450576affe70830644d01"
      }
    }
  },
  "value" : {
    "type" : "DefineToken",
    "value" : {
      "networkId" : 10,
      "createdAt" : "2021-11-12T11:46:29.862817Z",
      "definitionId" : "9bde938a6e8026362367faa1dd97ad7c7e14fafe535d025ed7fbbc36f0df7829",
      "name" : {
        "str" : "WONT",
        "bytes" : "V09OVA=="
      },
      "symbol" : {
        "str" : "WONT",
        "bytes" : "V09OVA=="
      },
      "divisionSize" : 0,
      "data" : ""
    }
  }
}""").toOption
      .get
  }

  implicit val updateState: UpdateState[Id] = new UpdateState[Id] {

    implicit def kvStore[K, V]: KeyValueStore[Id, K, V] =
      new KeyValueStore[Id, K, V] {

        private var store: Map[K, V] = Map.empty

        override def get(key: K): EitherT[Id, DecodingFailure, Option[V]] =
          EitherT.rightT[Id, DecodingFailure](store.get(key))
        override def put(key: K, value: V): Unit = store += (key -> value)
        override def remove(key: K): Unit        = store -= key
      }

    implicit def stateRepo[K, V] = StateRepository.fromStores[Id, K, V]

    implicit def txRepo: TransactionRepository[Id] =
      TransactionRepository.fromStores[Id]

    override def apply(
        state: MerkleState,
        tx: Signed.Tx,
    ): EitherT[Id, String, MerkleState] =
      StateService.updateStateWithTx[Id](state, tx)

  }

  test("onNewTx") {
    val Right(gossip1) = onNewTx[Id](initial, tx).value
    assertEquals(gossip1.newTxs.contains(tx.toHash), true)
  }

  test("generateNewBlockSuggestion") {
    val Right(gossip1) = onNewTx[Id](initial, tx).value
    val now            = Instant.parse("2021-11-11T15:48:40Z") // 1636645720000

    val Right((_, blockSuggestion)) =
      generateNewBlockSuggestion[Id](gossip1, now, params0).value

    assertEquals(blockSuggestion.transactionHashes, Set(tx.toHash))
  }

  test("onNewBlockSuggestion - self signed") {
    val Right(gossip1) = onNewTx[Id](initial, tx).value
    val now            = Instant.parse("2021-11-11T15:48:40Z") // 1636645720000
    val Right((blockHash, blockSuggestion)) =
      generateNewBlockSuggestion[Id](gossip1, now, params0).value

    val Right((gossip2, sigOption)) =
      onNewBlockSuggestion[Id](gossip1, blockSuggestion, params0).value

    assert(
      gossip2.newBlockSuggestions.contains(blockSuggestion.toHash),
      "In right time window, block suggestion expects to be accepted, but does not",
    )

    assertEquals(
      gossip2.bestBlock.map(_._1),
      Some(blockHash),
    )

    assertEquals(sigOption, None)
  }

  test("onNewBlockSuggestion - peer signed") {
    val Right(gossip1) = onNewTx[Id](initial, tx).value
    val now            = Instant.parse("2021-11-11T15:48:40Z") // 1636645720000
    val Right((_, blockSuggestion)) =
      generateNewBlockSuggestion[Id](gossip1, now, params0).value

    val Right((gossip2, sigOption)) =
      onNewBlockSuggestion[Id](gossip1, blockSuggestion, params1).value

    assert(
      gossip2.newBlockSuggestions.contains(blockSuggestion.toHash),
      "In right time window, block suggestion expects to be accepted, but does not",
    )

    assert(
      sigOption.nonEmpty,
      "Peer signed valid block suggestion expects to sign, but does not",
    )
  }

  test("onNewBlockSuggestion - wrong time window") {
    val Right(gossip1) = onNewTx[Id](initial, tx).value
    val now            = Instant.parse("2021-11-11T15:48:50Z") // 1636645730000
    val Right((_, blockSuggestion)) =
      generateNewBlockSuggestion[Id](gossip1, now, params0).value

    val blockEither =
      onNewBlockSuggestion[Id](gossip1, blockSuggestion, params0).value

    assert(
      blockEither.isLeft,
      "In wrong time window, block suggestion expects not to be accepted, but...",
    )
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

    ans match {
      case Right(gossip3) =>
        assertEquals(
          gossip3.newBlockVotes,
          Map((blockHash, params1.localNodeIndex) -> sig1),
        )
      case Left(msg) => fail(msg)
    }
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

    ans match {
      case Right(gossip3) =>
        assertEquals(
          gossip3.newBlockVotes,
          Map((blockHash, params0.localNodeIndex) -> sig0),
        )
        assertEquals(
          gossip3.bestBlock.map(_._1),
          Some(blockHash),
        )
      case Left(msg) => fail(msg)
    }
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
    assert(ans.isLeft, "suggestor's vote case expects to fail")
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
    val Right((gossip1_1@_, Some(sig1))) =
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

    ans match {
      case Right((gossip0_3, List((blockHash@_, block)))) =>
        assertEquals(gossip0_3.newBlockVotes.keySet, Set((blockHash, 1)))
        assertEquals(block.transactionHashes, Set(tx.toHash))
        assertEquals(block.votes, blockSuggestion.votes + sig1)
      case Right((_, blocks)) =>
        fail(s"Expected single block, got ${blocks.size}")
      case Left(msg) => fail(msg)
    }
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

    assertEquals(
      ans,
      Left("not enough number of vote to finalize: currently 0"),
    )
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

    ans match {
      case Left(msg) =>
        fail(msg)
      case Right((gossip6 @ _, blocks)) =>
        assertEquals(blocks.unzip._1, List(blockHash, blockHash2))
    }
  }


  test("tryFinalizeBlock " +
    "- remaining block suggestion states expect to be based on new confirmed state") {
    
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

    val Right((gossip1_1@_, Some(sig1_1))) =
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

    ans match {
      case Left(msg) =>
        fail(msg)
      case Right((gossip6@_, blocks)) =>
        assertEquals(blocks.unzip._1, List(blockHash))
        assertEquals(
          gossip6.newBlockSuggestions(blockHash2)._2.namesState.base,
          gossip2.newBlockSuggestions(blockHash)._2.namesState.root,
        )
        assertEquals(
          gossip6.newBlockSuggestions(blockHash2)._2.tokenState.base,
          gossip2.newBlockSuggestions(blockHash)._2.tokenState.root,
        )
        assertEquals(
          gossip6.newBlockSuggestions(blockHash2)._2.balanceState.base,
          gossip2.newBlockSuggestions(blockHash)._2.balanceState.root,
        )
    }
  }
}
