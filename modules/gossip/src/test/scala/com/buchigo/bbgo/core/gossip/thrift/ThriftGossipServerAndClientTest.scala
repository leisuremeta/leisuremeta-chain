package org.leisuremeta.lmchain.core
package gossip
package thrift

import java.time.Instant
import cats.data.NonEmptySeq
import cats.effect.IO
import scodec.bits.ByteVector
import crypto.Signature
import datatype.{BigNat, UInt256Refine}
import model._
import model.Transaction.Token

class ThriftGossipServerAndClientTest extends munit.FunSuite {

  val EmptyHeader = Block.Header(
    number = BigNat.Zero,
    parentHash = UInt256Refine.EmptyBytes.asInstanceOf[Block.BlockHash],
    namesRoot = None,
    tokenRoot = None,
    balanceRoot = None,
    transactionsRoot = None,
    timestamp = Instant.parse("2021-04-23T00:00:00Z"),
  )

  class EmptyGossipApi extends GossipApi[IO] {

    def bestConfirmedBlock: IO[Block.Header] = {
      IO.pure(EmptyHeader)
    }

    def block(blockHash: Block.BlockHash): IO[Option[Block]] = IO.pure(None)

    def nameState(
        blockHash: Block.BlockHash,
        from: Option[Account.Name],
        limit: Option[Int],
    ): IO[List[(Account.Name, NameState)]] = IO.pure(List.empty)

    def tokenState(
        blockHash: Block.BlockHash,
        from: Option[Token.DefinitionId],
        limit: Option[Int],
    ): IO[List[(Token.DefinitionId, TokenState)]] = IO.pure(List.empty)

    def balanceState(
        blockHash: Block.BlockHash,
        from: Option[(Account, Transaction.Input.Tx)],
        limit: Option[Int],
    ): IO[List[(Account, Transaction.Input.Tx)]] = IO.pure(List.empty)

    def newTxAndBlockSuggestions(
        bloomFilter: BloomFilter
    ): IO[(Set[Signed.Tx], Set[Block])] = {
      IO.pure((Set.empty, Set.empty))
    }

    def allNewTxAndBlockSuggestions: IO[(Set[Signed.Tx], Set[Block])] =
      IO.pure((Set.empty, Set.empty))
    def newTxAndBlockVotes(
        bloomFilter: BloomFilter,
        knownBlockVoteKeys: Set[(Block.BlockHash, Int)],
    ): IO[(Set[Signed.Tx], Map[(Block.BlockHash, Int), Signature])] =
      IO.pure((Set.empty, Map.empty))

    def allNewTxAndBlockVotes(
        knownBlockVoteKeys: Set[(Block.BlockHash, Int)]
    ): IO[(Set[Signed.Tx], Map[(Block.BlockHash, Int), Signature])] =
      IO.pure((Set.empty, Map.empty))

    def txs(txHashes: Set[Signed.TxHash]): IO[Set[Signed.Tx]] =
      IO.pure(Set.empty)
  }

  test("client") {
    val server = ThriftGossipServer.serve("localhost:40404")(new EmptyGossipApi)

    val client: GossipClient[IO] = new ThriftGossipClient[IO]("localhost:40404")

    val bestConfirmedBlock = client.bestConfirmedBlock.value.unsafeRunSync()

    server.close()

    assertEquals(bestConfirmedBlock, Right(EmptyHeader))
  }

  test("allNewTxAndBlockSuggestions") {
    val server = ThriftGossipServer.serve("localhost:40404")(new EmptyGossipApi)

    val client: GossipClient[IO] = new ThriftGossipClient[IO]("localhost:40404")

    val newTxAndBlockSuggestions =
      client.allNewTxAndBlockSuggestions.value.unsafeRunSync()

    server.close()

    val expected: Either[String, (Set[Signed[Transaction]], Set[Block])] =
      Right((Set.empty, Set.empty))

    assertEquals(newTxAndBlockSuggestions, expected)
  }

  test("newTxAndBlockSuggestions") {
    val server = ThriftGossipServer.serve("localhost:40404")(new EmptyGossipApi)

    val client: GossipClient[IO] = new ThriftGossipClient[IO]("localhost:40404")

    val Right(hash) = for {
      bytes <- ByteVector.fromHexDescriptive("d485885fae7c1580b0ffaa7c9a00ecbd1957fd58b6f08dbe87590174a26b72f9")
      refined <- UInt256Refine.from(bytes)
    } yield refined

    val bloomFilter = BloomFilter.from(NonEmptySeq.one(hash))

    val newTxAndBlockSuggestions =
      client.newTxAndBlockSuggestions(
        bloomFilter = bloomFilter,
      ).value.unsafeRunSync()

    server.close()

    val expected: Either[String, (Set[Signed[Transaction]], Set[Block])] =
      Right((Set.empty, Set.empty))

    assertEquals(newTxAndBlockSuggestions, expected)
  }
}
