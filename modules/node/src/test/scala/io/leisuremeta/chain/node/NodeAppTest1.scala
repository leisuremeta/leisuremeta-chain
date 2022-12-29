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
import scodec.bits.hex
import sttp.client3.*
import sttp.model.{MediaType, StatusCode}

import api.model.*
import api_model.AccountInfo
import api.model.token.*
import lib.crypto.{CryptoOps, Hash}
import lib.crypto.Hash.ops.*
import lib.crypto.Sign.ops.*
import lib.datatype.{BigNat, UInt256, Utf8}
import lib.failure.DecodingFailure
import lib.merkle.{MerkleTrieNode, MerkleTrieState}
import lib.merkle.MerkleTrieNode.MerkleRoot
import repository.{BlockRepository, StateRepository, TransactionRepository}
import service.LocalGossipService
import service.interpreter.LocalGossipServiceInterpreter

import hedgehog.munit.HedgehogSuite
import hedgehog.*

class NodeAppTest1 extends HedgehogSuite:

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

  given testStateRepo[K, V]: StateRepository[IO, K, V] =
    StateRepository.fromStores[IO, K, V]

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
                Right(expectedNodeStatus),
              )
            }
          }
        }
        .unsafeRunSync()
    }
  }

  test("define group is defined") {
    withMunitAssertions { assertions =>

      val account = Account(Utf8.unsafeFrom("alice"))

      val keyPair = CryptoOps.fromPrivate(
        BigInt(
          "f7f0bad6ea0f32173c539a3d38913fd4b221a8a4d709197f2f83a05e62f9f602",
          16,
        ),
      )

      given bodyJsonSerializer[A: Encoder]: BodySerializer[A] =
        (a: A) =>
          val serialized = a.asJson.noSpaces
          StringBody(serialized, "UTF-8", MediaType.ApplicationJson)

      val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()

      def sign(tx: Transaction): Signed.Tx =
        val Right(sig) = keyPair.sign(tx): @unchecked
        Signed(AccountSignature(sig, account), tx)

      def submit(tx: Transaction) =
        val Right(sig) = keyPair.sign(tx): @unchecked

//      given io.circe.Encoder[Transaction] = {
//        import io.circe.generic.semiauto.*
//        deriveEncoder[Transaction]
//      }

        basicRequest
          .response(asStringAlways)
          .post(uri"http://localhost:8081/tx")
          .body(Seq(Signed(AccountSignature(sig, account), tx)))
          .send(backend)

      val tx0: Transaction = Transaction.AccountTx.CreateAccount(
        networkId = NetworkId(BigNat.unsafeFromLong(1000L)),
        createdAt = Instant.parse("2020-05-22T09:00:00.00Z"),
        account = account,
        ethAddress = None,
        guardian = None,
      )

      val tx1: Transaction = Transaction.GroupTx.CreateGroup(
        networkId = NetworkId(BigNat.unsafeFromLong(1000L)),
        createdAt = Instant.parse("2020-05-22T09:01:00.00Z"),
        groupId = GroupId(Utf8.unsafeFrom("group-core")),
        name = Utf8.unsafeFrom("group-core"),
        coordinator = account,
      )

      val tx2: Transaction = Transaction.GroupTx.AddAccounts(
        networkId = NetworkId(BigNat.unsafeFromLong(1000L)),
        createdAt = Instant.parse("2020-05-22T09:02:00.00Z"),
        groupId = GroupId(Utf8.unsafeFrom("group-core")),
        accounts = Set(account),
      )

      val tx3: Transaction = Transaction.TokenTx.DefineToken(
        networkId = NetworkId(BigNat.unsafeFromLong(1000L)),
        createdAt = Instant.parse("2020-05-22T09:03:00.00Z"),
        definitionId = TokenDefinitionId(Utf8.unsafeFrom("token-core")),
        name = Utf8.unsafeFrom("token-core"),
        symbol = Some(Utf8.unsafeFrom("TC")),
        minterGroup = Some(GroupId(Utf8.unsafeFrom("group-core"))),
        nftInfo = Some(
          NftInfo(
            minter = account,
            rarity = Map(
              Rarity(Utf8.unsafeFrom("LEGENDARY")) -> BigNat.unsafeFromLong(8L),
              Rarity(Utf8.unsafeFrom("UNIQUE"))    -> BigNat.unsafeFromLong(6L),
              Rarity(Utf8.unsafeFrom("EPIC"))      -> BigNat.unsafeFromLong(4L),
              Rarity(Utf8.unsafeFrom("RARE"))      -> BigNat.unsafeFromLong(2L),
            ),
            dataUrl = Utf8.unsafeFrom("https://example.com/data"),
            contentHash = UInt256
              .from(
                hex"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
              )
              .toOption
              .get,
          ),
        ),
      )

      val tx4: Transaction = Transaction.TokenTx.MintNFT(
        networkId = NetworkId(BigNat.unsafeFromLong(1000L)),
        createdAt = Instant.parse("2020-05-22T09:04:00.00Z"),
        tokenDefinitionId = TokenDefinitionId(Utf8.unsafeFrom("token-core")),
        tokenId = TokenId(Utf8.unsafeFrom("2022061710000513118")),
        rarity = Rarity(Utf8.unsafeFrom("EPIC")),
        dataUrl = Utf8.unsafeFrom(
          "https://d3j8b1jkcxmuqq.cloudfront.net/temp/collections/TEST_NOMM4/NFT_ITEM/F7A92FB1-B29F-4E6F-BEF1-47C6A1376D68.jpg",
        ),
        contentHash = UInt256
          .from(
            hex"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
          )
          .toOption
          .get,
        output = account,
      )

      getApp.resource
        .flatMap { appResource =>
          appResource.use { _ =>
            IO({
              List(tx0, tx1, tx2, tx3, tx4).foreach { tx =>
                println(
                  s"===> submitting tx: $tx",
                )
                val response = submit(tx)
                println(
                  s"submit tx result: body: ${response.body}, status code: ${response.code}",
                )

                assertions.assertEquals(response.code, StatusCode.Ok)
              }
            })
          }
        }
        .unsafeRunSync()
    }
  }
