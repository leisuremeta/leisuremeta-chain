package io.leisuremeta.chain
package node

import java.time.Instant

import cats.data.EitherT
import cats.effect.{IO, Ref}
import cats.effect.unsafe.implicits.global

import com.typesafe.config.ConfigFactory
import io.circe.generic.auto.given
import io.circe.parser.decode
import io.circe.refined.given
import scodec.bits.ByteVector
import sttp.client3.*
import sttp.model.StatusCode

import api.model.{Block, NetworkId, NodeStatus, StateRoot, Signed}
import lib.crypto.{CryptoOps, Hash}
import lib.crypto.Hash.ops.*
import lib.datatype.{BigNat, UInt256}
import lib.failure.DecodingFailure
import repository.BlockRepository

import minitest.SimpleTestSuite
import hedgehog.minitest.HedgehogSupport
import hedgehog.*

object NodeAppTest extends SimpleTestSuite with HedgehogSupport:

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

  given testBlockRepo: BlockRepository[IO] = new BlockRepository[IO]:
    private val _bestHeader: Ref[IO, Option[Block.Header]] =
      Ref.unsafe[IO, Option[Block.Header]](None)

    private val map: scala.collection.mutable.Map[Hash.Value[Block], Block] =
      scala.collection.mutable.Map.empty
    def bestHeader: EitherT[IO, DecodingFailure, Option[Block.Header]] =
      EitherT.right[DecodingFailure](_bestHeader.get)
    def get(
        hash: Hash.Value[Block],
    ): EitherT[IO, DecodingFailure, Option[Block]] = EitherT.pure[IO, DecodingFailure](map.get(hash))
    def put(block: Block): EitherT[IO, DecodingFailure, Unit] = EitherT.right{
      IO(map += (block.toHash -> block)) *> _bestHeader.update{
        case Some(bestHeader) if block.header.number.toBigInt <= bestHeader.number.toBigInt =>
          Some(bestHeader)
        case _ =>
          Some(block.header)
      }
    }

    def listFrom(
        blockNumber: BigNat,
        limit: Int,
    ): EitherT[IO, DecodingFailure, List[(BigNat, Block.BlockHash)]] = ???
    def findByTransaction(
        txHash: Signed.TxHash,
    ): EitherT[IO, DecodingFailure, Option[Block.BlockHash]] = ???

  example("app generate genesis block in initialization") {

    val Right(conf) = NodeConfig
      .load(IO(ConfigFactory.parseString(confString)))
      .value
      .unsafeRunSync()

    println(s"Conf: $conf")

    val app = NodeApp[IO](conf)

    val resultIO = app.resource.use{ _ =>
      IO {
        val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()
        def request =
          val result = basicRequest
            .response(asStringAlways)
            .get(uri"http://localhost:8081/status")
            .send(backend)
          println(
            s"status request result: body: ${result.body}, status code: ${result.code}",
          )
          result

        Result.all(
          List(
            request.code ==== StatusCode.Ok,
            decode[NodeStatus](request.body) ==== Right(expectedNodeStatus),
          ),
        )
      }
    }

    resultIO.unsafeRunSync()
  }
