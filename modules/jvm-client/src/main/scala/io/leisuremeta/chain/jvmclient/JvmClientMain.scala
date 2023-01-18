package io.leisuremeta.chain
package jvmclient

import cats.effect.{ExitCode, IO, IOApp}
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.generic.auto.*
import io.circe.syntax.*
import scodec.bits.hex
import sttp.client3.*
import sttp.client3.armeria.cats.ArmeriaCatsBackend
import sttp.tapir.client.sttp.SttpClientInterpreter

import api.LeisureMetaChainApi
import api.model.*
import api.model.reward.*
import api.model.token.*
import lib.datatype.*
import lib.crypto.*
import lib.crypto.Hash.ops.*
import lib.crypto.Sign.ops.*
import node.NodeConfig
import scodec.bits.ByteVector


object JvmClientMain extends IOApp:


  java.security.Security.addProvider(
    new org.bouncycastle.jce.provider.BouncyCastleProvider(),
  )

  val aliceKey = CryptoOps.fromPrivate(
    BigInt(
      "b229e76b742616db3ac2c5c2418f44063fcc5fcc52a08e05d4285bdb31acba06",
      16,
    ),
  )
  val alicePKS = PublicKeySummary.fromPublicKeyHash(aliceKey.publicKey.toHash)
  val alice    = Account(Utf8.unsafeFrom("alice"))
  val bob    = Account(Utf8.unsafeFrom("bob"))
  val carol  = Account(Utf8.unsafeFrom("carol"))

  def sign(account: Account, key: KeyPair)(tx: Transaction): Signed.Tx =
    key.sign(tx).map { sig =>
      Signed(AccountSignature(sig, account), tx)
    } match
      case Right(signedTx) => signedTx
      case Left(msg)       => throw Exception(msg)

  def signAlice = sign(alice, aliceKey)

  val like = Utf8.unsafeFrom("like")

  val tx: Transaction = Transaction.RewardTx.RecordActivity(
    networkId = NetworkId(BigNat.unsafeFromLong(2021L)),
    createdAt = java.time.Instant.parse("2023-01-10T18:01:00.00Z"),
    timestamp = java.time.Instant.parse("2023-01-09T09:00:00.00Z"),
    userActivity = Map(
      bob -> Map(
        like -> DaoActivity(BigInt(1), BigNat.unsafeFromLong(3)),
      ),
      carol -> Map(
        like -> DaoActivity(BigInt(1), BigNat.unsafeFromLong(3)),
      ),
    ),
    tokenReceived = Map(
      TokenId(Utf8.unsafeFrom("text-20230109-0000")) -> Map(
        like -> DaoActivity(BigInt(1), BigNat.unsafeFromLong(2)),
      ),
      TokenId(Utf8.unsafeFrom("text-20230109-0001")) -> Map(
        like -> DaoActivity(BigInt(1), BigNat.unsafeFromLong(2)),
      ),
      TokenId(Utf8.unsafeFrom("text-20230109-0002")) -> Map(
        like -> DaoActivity(BigInt(1), BigNat.unsafeFromLong(2)),
      ),
    ),
  )

//  val tx: Transaction = Transaction.RewardTx.BuildSnapshot(
//    networkId = NetworkId(BigNat.unsafeFromLong(2021L)),
//    createdAt = java.time.Instant.parse("2023-01-11T18:01:00.00Z"),
//    timestamp = java.time.Instant.parse("2023-01-09T09:00:00.00Z"),
//  )

//  val lm = TokenDefinitionId(Utf8.unsafeFrom("LM"))
//
//  def txHash(bytes: ByteVector): Signed.TxHash =
//    Hash.Value[Signed.Tx](UInt256.from(bytes).toOption.get)
//
//  val tx: Transaction = Transaction.RewardTx.ExecuteAccountReward(
//    networkId = NetworkId(BigNat.unsafeFromLong(2021L)),
//    createdAt = java.time.Instant.parse("2023-01-11T18:01:00.00Z"),
//    inputDefinitionId = lm,
//    inputs = Set(
//      txHash(hex"270650f92f584d9dbbffb99f3a915dc908fbea28bc3dbf34b8cdbe49c4070611"),
//    ),
//    targets = Set(alice, bob, carol),
//    amountPerPoint = BigNat.unsafeFromLong(10),
//  )
    
  val signedTx = signAlice(tx)

  val json = Seq(signedTx).asJson.spaces2

  override def run(args: List[String]): IO[ExitCode] =
    ArmeriaCatsBackend.resource[IO]().use { backend =>

      val loadConfig = IO.blocking(ConfigFactory.load)

      NodeConfig.load[IO](loadConfig).value.flatMap {
        case Right(config) =>
          val baseUri = uri"http://localhost:${config.local.port}"
          val postTxClient = SttpClientInterpreter().toClient(
            LeisureMetaChainApi.postTxEndpoint,
            Some(baseUri),
            backend,
          )

          println(json)
          println(Seq(tx.toHash).asJson.noSpaces)

          IO.unit.as(ExitCode.Success)

//          for response <- postTxClient(Seq(signedTx))
//          yield
//            println(response)
//            ExitCode.Success

        case Left(err) =>
          IO.println(err).as(ExitCode.Error)
      }
    }
