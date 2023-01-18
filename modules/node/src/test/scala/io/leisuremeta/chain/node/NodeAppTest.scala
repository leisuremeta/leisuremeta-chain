package io.leisuremeta.chain
package node

import java.time.Instant

import cats.data.EitherT
import cats.effect.{IO, Ref}
import cats.effect.unsafe.implicits.global

import com.typesafe.config.ConfigFactory
import io.circe.Encoder
import io.circe.generic.auto.given
import io.circe.parser.decode
import io.circe.refined.given
import io.circe.syntax.given
import scodec.bits.ByteVector
import sttp.client3.*
import sttp.model.{MediaType, StatusCode}

import api.model.*
import api_model.AccountInfo
import api.model.token.TokenDefinitionId
import lib.crypto.{CryptoOps, Hash}
import lib.crypto.Hash.ops.*
import lib.crypto.Sign.ops.*
import lib.datatype.{BigNat, UInt256, Utf8}
import lib.failure.DecodingFailure
import lib.merkle.{GenericMerkleTrieNode, GenericMerkleTrieState}
import lib.merkle.GenericMerkleTrieNode.MerkleRoot
import repository.{BlockRepository, GenericStateRepository, TransactionRepository}
import service.LocalGossipService
import service.interpreter.LocalGossipServiceInterpreter

import hedgehog.munit.HedgehogSuite
import hedgehog.*

class NodeAppTest extends HedgehogSuite:

  val confString = """
local {
  network-id = 1000,
  port = 8081,
  private = "f7f0bad6ea0f32173c539a3d38913fd4b221a8a4d709197f2f83a05e62f9f602", // local private key (Hex)  
  //0x9566d9a539479cda8c3d982251c863b9e3153eeb0956d2d6690485d8acc386d3002308dccf754f4cdbd8bb860bec4fb3b29b0c705d703a0c81abe54365db45ab
  //0xbe5c3336eaf666ae9659b15b4bf63b9b5a315a36
}
wire {
  time-window-millis = 1000,
  port = 11111,
  peers: [{
    dest: "localhost: 8081",    // "address:port"
    public-key-summary: "be5c3336eaf666ae9659b15b4bf63b9b5a315a36", // Peer Public Key Summary (Hex)
  }],
}
genesis {
  timestamp: "2020-05-20T09:00:00.00Z",
}
"""

  val expectedGenesisBlock = Block(
    header = Block.Header(
      number = BigNat.Zero,
      parentHash = Hash.Value[Block](UInt256.EmptyBytes),
      stateRoot = StateRoot.empty,
      transactionsRoot = None,
      timestamp = Instant.parse("2020-05-20T09:00:00.00Z"),
    ),
    transactionHashes = Set.empty,
    votes = Set.empty,
  )

  val expectedGenesisBlockHash = expectedGenesisBlock.toHash

  val expectedNodeStatus = NodeStatus(
    networkId = NetworkId(BigNat.unsafeFromLong(1000L)),
    genesisHash = expectedGenesisBlockHash,
    bestHash = expectedGenesisBlockHash,
    number = BigNat.Zero,
  )

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

  given testBlockRepo: BlockRepository[IO] = new BlockRepository[IO]:
    private val _bestHeader: Ref[IO, Option[Block.Header]] =
      Ref.unsafe[IO, Option[Block.Header]](None)

    private val map: scala.collection.mutable.Map[Hash.Value[Block], Block] =
      scala.collection.mutable.Map.empty

    def bestHeader: EitherT[IO, DecodingFailure, Option[Block.Header]] =
      EitherT.right[DecodingFailure](_bestHeader.get)
    def get(
        hash: Hash.Value[Block],
    ): EitherT[IO, DecodingFailure, Option[Block]] =
      EitherT.pure[IO, DecodingFailure](map.get(hash))
    def put(block: Block): EitherT[IO, DecodingFailure, Unit] = EitherT.right {
      IO(map += (block.toHash -> block)) *> _bestHeader.update {
        case Some(bestHeader)
            if block.header.number.toBigInt <= bestHeader.number.toBigInt =>
          Some(bestHeader)
        case _ =>
          Some(block.header)
      }
    }

    def listFrom(
        blockNumber: BigNat,
        limit: Int,
    ): EitherT[IO, DecodingFailure, List[(BigNat, Block.BlockHash)]] =
      println(s"======> ListFrom is called!!")
      ???

    def findByTransaction(
        txHash: Signed.TxHash,
    ): EitherT[IO, DecodingFailure, Option[Block.BlockHash]] =
      println(s"======> findByTransaction is called!!")
      ???

  given testTxRepo: TransactionRepository[IO] = new TransactionRepository[IO]:

    private val map: scala.collection.mutable.Map[Hash.Value[
      TransactionWithResult,
    ], TransactionWithResult] =
      scala.collection.mutable.Map.empty

    def get(
        transactionHash: Hash.Value[TransactionWithResult],
    ): EitherT[IO, DecodingFailure, Option[TransactionWithResult]] =
      scribe.info(s"======> get is called with tx hash: ${transactionHash}")
      EitherT.pure[IO, DecodingFailure](map.get(transactionHash))

    def put(transaction: TransactionWithResult): IO[Unit] = IO {
      scribe.info(s"putting transaction: $transaction")
      map += transaction.toHash -> transaction
    }

  given testStateRepo[K, V]: GenericStateRepository[IO, K, V] =
    GenericStateRepository.fromStores[IO, K, V]

  given LocalGossipService[IO] = LocalGossipServiceInterpreter
    .build[IO](
      bestConfirmedBlock = ???,
      params = ???,
    )
    .unsafeRunSync()

  def getApp: NodeApp[IO] =
    val Right(conf) = NodeConfig
      .load(IO(ConfigFactory.parseString(confString)))
      .value
      .unsafeRunSync(): @unchecked

    NodeApp[IO](conf)

  test("app generate genesis block in initialization") {
    withMunitAssertions { assertions =>
      getApp.resource
        .flatMap { appResource =>
          appResource.use { _ =>
            IO {
              val backend: SttpBackend[Identity, Any] =
                HttpURLConnectionBackend()

              val response = basicRequest
                .response(asStringAlways)
                .get(uri"http://localhost:8081/status")
                .send(backend)
              println(
                s"status request result: body: ${response.body}, status code: ${response.code}",
              )

              assertions.assertEquals(response.code, StatusCode.Ok)
              assertions.assertEquals(
                decode[NodeStatus](response.body),
                Right(
                  expectedNodeStatus,
                ),
              )
            }
          }
        }
        .unsafeRunSync()
    }
  }

  test("post tx is defined") {
    withMunitAssertions { assertions =>

      val account = Account(Utf8.unsafeFrom("alice"))

      val tx: Transaction = Transaction.AccountTx.CreateAccount(
        networkId = NetworkId(BigNat.unsafeFromLong(1000L)),
        createdAt = Instant.parse("2020-05-22T09:00:00.00Z"),
        account = account,
        ethAddress = None,
        guardian = None,
      )

      val txHash = tx.toHash

      val keyPair = CryptoOps.fromPrivate(
        BigInt(
          "f7f0bad6ea0f32173c539a3d38913fd4b221a8a4d709197f2f83a05e62f9f602",
          16,
        ),
      )

      val Right(sig) = keyPair.sign(tx): @unchecked

      val accountSig = AccountSignature(sig, account)

      val signedTx = Signed(accountSig, tx)

      given bodyJsonSerializer[A: Encoder]: BodySerializer[A] =
        (a: A) =>
          val serialized = a.asJson.noSpaces
          StringBody(serialized, "UTF-8", MediaType.ApplicationJson)

      getApp.resource
        .flatMap { appResource =>
          appResource.use { _ =>
            IO {
              val backend: SttpBackend[Identity, Any] =
                HttpURLConnectionBackend()
              val response0 = basicRequest
                .response(asStringAlways)
                .post(uri"http://localhost:8081/tx")
                .body(Seq(signedTx))
                .send(backend)
              println(
                s"post tx request result: body: ${response0.body}, status code: ${response0.code}",
              )
              val response1 = basicRequest
                .response(asStringAlways)
                .get(
                  uri"http://localhost:8081/tx/${txHash.toUInt256Bytes.toBytes.toHex}",
                )
                .send(backend)
              println(
                s"get tx request result: body: ${response1.body}, status code: ${response1.code}",
              )
              val response2 = basicRequest
                .response(asStringAlways)
                .get(uri"http://localhost:8081/account/alice")
                .send(backend)

              println(
                s"get account request result: body: ${response2.body}, status code: ${response2.code}",
              )

              assertions.assertEquals(response0.code, StatusCode.Ok)
              assertions.assertEquals(
                decode[Seq[Signed.TxHash]](response0.body),
                Right(Seq(signedTx.toHash)),
              )
              assertions.assertEquals(response1.code, StatusCode.Ok)
              assertions.assertEquals(response2.code, StatusCode.Ok)
              assertions.assertEquals(
                decode[AccountInfo](response2.body)
                  .map(_.publicKeySummaries.nonEmpty),
                Right(true),
              )
            }
          }
        }
        .unsafeRunSync()
    }
  }

  test("get group is defined") {
    withMunitAssertions { assertions =>

      val account = Account(Utf8.unsafeFrom("alice"))

      val tx: Transaction = Transaction.AccountTx.CreateAccount(
        networkId = NetworkId(BigNat.unsafeFromLong(1000L)),
        createdAt = Instant.parse("2020-05-22T09:00:00.00Z"),
        account = account,
        ethAddress = None,
        guardian = None,
      )

      val txHash = tx.toHash

      val keyPair = CryptoOps.fromPrivate(
        BigInt(
          "f7f0bad6ea0f32173c539a3d38913fd4b221a8a4d709197f2f83a05e62f9f602",
          16,
        ),
      )

      val Right(sig) = keyPair.sign(tx): @unchecked

      val accountSig = AccountSignature(sig, account)

      val signedTx = Signed(accountSig, tx)

      val tx2: Transaction = Transaction.GroupTx.CreateGroup(
        networkId = NetworkId(BigNat.unsafeFromLong(1000L)),
        createdAt = Instant.parse("2020-05-22T09:01:00.00Z"),
        groupId = GroupId(Utf8.unsafeFrom("group-core")),
        name = Utf8.unsafeFrom("group-core"),
        coordinator = account,
      )
      val Right(sig2) = keyPair.sign(tx2): @unchecked
      val accountSig2 = AccountSignature(sig2, account)
      val signedTx2   = Signed(accountSig2, tx2)

      given bodyJsonSerializer[A: Encoder]: BodySerializer[A] =
        (a: A) =>
          val serialized = a.asJson.noSpaces
          StringBody(serialized, "UTF-8", MediaType.ApplicationJson)

      getApp.resource
        .flatMap { appResource =>
          appResource.use { _ =>
            IO {
              val backend: SttpBackend[Identity, Any] =
                HttpURLConnectionBackend()
              val response0 = basicRequest
                .response(asStringAlways)
                .post(uri"http://localhost:8081/tx")
                .body(Seq(signedTx))
                .send(backend)
              println(
                s"post tx request result: body: ${response0.body}, status code: ${response0.code}",
              )
              val response1 = basicRequest
                .response(asStringAlways)
                .post(uri"http://localhost:8081/tx")
                .body(Seq(signedTx2))
                .send(backend)
              println(
                s"post tx request result: body: ${response1.body}, status code: ${response1.code}",
              )
//            val response2 = basicRequest
//              .response(asStringAlways)
//              .get(uri"http://localhost:8081/group/group-core")
//              .send(backend)
//
//            println(
//              s"get account request result: body: ${response2.body}, status code: ${response2.code}",
//            )

//              assertions.assertEquals(response0.code, StatusCode.Ok)
//              assertions.assertEquals(
//                decode[Seq[Signed.TxHash]](response0.body),
//                Right(Seq(signedTx.toHash)),
//              )
              assertions.assertEquals(response1.code, StatusCode.Ok)
//              assertions.assertEquals(response2.code, StatusCode.Ok)
//              assertions.assertEquals(
//                decode[AccountInfo](response2.body)
//                  .map(_.publicKeySummaries.nonEmpty),
//                Right(true),
//              )
            }
          }
        }
        .unsafeRunSync()
    }
  }
/*
  test("get token definition is defined") {

    val account = Account(Utf8.unsafeFrom("alice"))

    val tx: Transaction = Transaction.AccountTx.CreateAccount(
      networkId = NetworkId(BigNat.unsafeFromLong(1000L)),
      createdAt = Instant.parse("2020-05-22T09:00:00.00Z"),
      account = account,
      guardian = None,
    )

    val txHash = tx.toHash

    val keyPair = CryptoOps.fromPrivate(
      BigInt(
        "f7f0bad6ea0f32173c539a3d38913fd4b221a8a4d709197f2f83a05e62f9f602",
        16,
      ),
    )

    val Right(sig) = keyPair.sign(tx)

    val accountSig = AccountSignature(sig, account)

    val signedTx = Signed(accountSig, tx)

    val tx2: Transaction = Transaction.TokenTx.DefineToken(
      networkId = NetworkId(BigNat.unsafeFromLong(1000L)),
      createdAt = Instant.parse("2020-05-22T09:01:00.00Z"),
      definitionId = TokenDefinitionId(Utf8.unsafeFrom("test-token")),
      name = Utf8.unsafeFrom("test-token"),
      symbol = Some(Utf8.unsafeFrom("TT")),
      minterGroup = None,
      nftInfo =  None,
    )

    val Right(sig2) = keyPair.sign(tx2)
    val accountSig2 = AccountSignature(sig2, account)
    val signedTx2   = Signed(accountSig2, tx2)

    given bodyJsonSerializer[A: Encoder]: BodySerializer[A] =
      (a: A) =>
        val serialized = a.asJson.noSpaces
        StringBody(serialized, "UTF-8", MediaType.ApplicationJson)

    getApp.resource
      .flatMap { appResource =>
        appResource.use { _ =>
          IO {
            val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()
            val response0 = basicRequest
              .response(asStringAlways)
              .post(uri"http://localhost:8081/tx")
              .body(Seq(signedTx))
              .send(backend)
            println(
              s"===> post tx0 request result: body: ${response0.body}, status code: ${response0.code}",
            )
            val response1 = basicRequest
              .response(asStringAlways)
              .post(uri"http://localhost:8081/tx")
              .body(Seq(signedTx2))
              .send(backend)
            println(
              s"===> post tx2 request result: body: ${response1.body}, status code: ${response1.code}",
            )
            val response2 = basicRequest
              .response(asStringAlways)
              .get(uri"http://localhost:8081/token-def/test-token")
              .send(backend)

            println(
              s"===> get token-def request result: body: ${response2.body}, status code: ${response2.code}",
            )

            Result.all(
              List(
                response0.code ==== StatusCode.Ok,
                decode[Seq[Signed.TxHash]](response0.body) ==== Right(
                  Seq(tx.toHash),
                ),
                response1.code ==== StatusCode.Ok,
                response2.code ==== StatusCode.Ok,
//                decode[AccountInfo](response2.body)
//                  .map(_.publicKeySummaries.nonEmpty) ==== Right(true),
              ),
            )
          }
        }
      }
      .unsafeRunSync()
  }
 */
