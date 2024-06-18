package io.leisuremeta.chain
package jvmclient

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.traverse.*
import com.typesafe.config.ConfigFactory
import io.circe.syntax.*
import scodec.bits.hex
import sttp.client3.*
import sttp.client3.armeria.cats.ArmeriaCatsBackend
//import sttp.tapir.client.sttp.SttpClientInterpreter

//import api.LeisureMetaChainApi
import api.model.*
//import api.model.reward.*
import api.model.token.*
//import api.model.TransactionWithResult.ops.*
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
  val bob      = Account(Utf8.unsafeFrom("bob"))
  val carol    = Account(Utf8.unsafeFrom("carol"))

  def sign(account: Account, key: KeyPair)(tx: Transaction): Signed.Tx =
    key.sign(tx).map { sig =>
      Signed(AccountSignature(sig, account), tx)
    } match
      case Right(signedTx) => signedTx
      case Left(msg)       => throw Exception(msg)

  def signAlice = sign(alice, aliceKey)

  val txs: IndexedSeq[Transaction] = IndexedSeq(
    Transaction.AccountTx.CreateAccount(
      networkId = NetworkId(BigNat.unsafeFromLong(2021L)),
      createdAt = java.time.Instant.parse("2023-01-11T19:01:00.00Z"),
      account = alice,
      ethAddress = None,
      guardian = None,
//      memo = None,
    ),
    Transaction.GroupTx.CreateGroup(
      networkId = NetworkId(BigNat.unsafeFromLong(2021L)),
      createdAt = java.time.Instant.parse("2023-01-11T19:02:00.00Z"),
      groupId = GroupId(Utf8.unsafeFrom("mint-group")),
      name = Utf8.unsafeFrom("Mint Group"),
      coordinator = alice,
//      memo = None,
    ),
    Transaction.GroupTx.AddAccounts(
      networkId = NetworkId(BigNat.unsafeFromLong(2021L)),
      createdAt = java.time.Instant.parse("2023-01-11T19:03:00.00Z"),
      groupId = GroupId(Utf8.unsafeFrom("mint-group")),
      accounts = Set(alice),
//      memo = None,
    ),
    Transaction.TokenTx.DefineToken(
      networkId = NetworkId(BigNat.unsafeFromLong(2021L)),
      createdAt = java.time.Instant.parse("2023-01-11T19:04:00.00Z"),
      definitionId = TokenDefinitionId(Utf8.unsafeFrom("LM")),
      name = Utf8.unsafeFrom("LeisureMeta"),
      symbol = Some(Utf8.unsafeFrom("LM")),
      minterGroup = Some(GroupId(Utf8.unsafeFrom("mint-group"))),
      nftInfo = None,
    ),
    Transaction.TokenTx.DefineTokenWithPrecision(
      networkId = NetworkId(BigNat.unsafeFromLong(2021L)),
      createdAt = java.time.Instant.parse("2023-01-11T19:05:00.00Z"),
      definitionId = TokenDefinitionId(Utf8.unsafeFrom("nft-with-precision")),
      name = Utf8.unsafeFrom("NFT with precision"),
      symbol = Some(Utf8.unsafeFrom("NFTWP")),
      minterGroup = Some(GroupId(Utf8.unsafeFrom("mint-group"))),
      nftInfo = Some(
        NftInfoWithPrecision(
          minter = alice,
          rarity = Map(
            Rarity(Utf8.unsafeFrom("LGDY")) -> BigNat.unsafeFromLong(100),
            Rarity(Utf8.unsafeFrom("UNIQ")) -> BigNat.unsafeFromLong(66),
            Rarity(Utf8.unsafeFrom("EPIC")) -> BigNat.unsafeFromLong(33),
            Rarity(Utf8.unsafeFrom("RARE")) -> BigNat.unsafeFromLong(10),
          ),
          precision = BigNat.unsafeFromLong(2),
          dataUrl = Utf8.unsafeFrom(
            "https://www.playnomm.com/data/nft-with-precision.json",
          ),
          contentHash = UInt256
            .from(
              hex"2475a387f22c248c5a3f09cea0ef624484431c1eaf8ffbbf98a4a27f43fabc84",
            )
            .toOption
            .get,
        ),
      ),
//      memo = None,
    ),
    Transaction.TokenTx.MintNFT(
      networkId = NetworkId(BigNat.unsafeFromLong(2021L)),
      createdAt = java.time.Instant.parse("2023-01-11T19:06:00.00Z"),
      tokenDefinitionId =
        TokenDefinitionId(Utf8.unsafeFrom("nft-with-precision")),
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
      output = alice,
//      memo = None,
    ),
    Transaction.TokenTx.MintNFTWithMemo(
      networkId = NetworkId(BigNat.unsafeFromLong(2021L)),
      createdAt = java.time.Instant.parse("2023-01-11T19:07:00.00Z"),
      tokenDefinitionId =
        TokenDefinitionId(Utf8.unsafeFrom("nft-with-precision")),
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
      output = alice,
      memo = Some(Utf8.unsafeFrom("Test Minting NFT #2022061710000513118")),
    ),
    Transaction.TokenTx.UpdateNFT(
      networkId = NetworkId(BigNat.unsafeFromLong(2021L)),
      createdAt = java.time.Instant.parse("2023-01-11T19:08:00.00Z"),
      tokenDefinitionId =
        TokenDefinitionId(Utf8.unsafeFrom("nft-with-precision")),
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
      output = alice,
      memo = Some(Utf8.unsafeFrom("Test Updating NFT #2022061710000513118")),
    ),
  )

  override def run(args: List[String]): IO[ExitCode] =
    ArmeriaCatsBackend.resource[IO]().use { backend =>

      val loadConfig = IO.blocking(ConfigFactory.load)

      NodeConfig.load[IO](loadConfig).value.flatMap {
        case Right(config) =>
//          val baseUri = uri"http://localhost:${config.local.port}"
//          val postTxClient = SttpClientInterpreter().toClient(
//            LeisureMetaChainApi.postTxEndpoint,
//            Some(baseUri),
//            backend,
//          )

          txs.toList
            .traverse: tx =>
              val signedTx = signAlice(tx)
              val json     = Seq(signedTx).asJson.spaces2
              println(json)
              println(Seq(tx.toHash).asJson.noSpaces)
              IO.unit

//              for response <-
//                postTxClient(Seq(signedTx))
//              yield
//                println(response)
//                ExitCode.Success
            .as(ExitCode.Success)

        case Left(err) =>
          IO.println(err).as(ExitCode.Error)
      }
    }
