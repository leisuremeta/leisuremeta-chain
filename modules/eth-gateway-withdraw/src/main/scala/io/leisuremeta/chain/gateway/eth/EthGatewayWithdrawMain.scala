package io.leisuremeta.chain
package gateway.eth

import java.math.BigInteger
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.time.Instant
import java.util.{ArrayList, Arrays, Collections}

import scala.concurrent.duration.*
import scala.jdk.OptionConverters.*
import scala.util.Try

import cats.data.OptionT
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.eq.*
import cats.syntax.traverse.*

import com.typesafe.config.{Config, ConfigFactory}
import io.circe.Encoder
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.given
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.web3j.abi.{FunctionEncoder, TypeReference}
import org.web3j.abi.datatypes.{Address, Function, Type}
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.contracts.eip20.generated.ERC20
import org.web3j.contracts.eip721.generated.ERC721
import org.web3j.crypto.{Credentials, RawTransaction, TransactionEncoder}
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.gas.{DefaultGasProvider, StaticGasProvider}
import org.web3j.tx.response.PollingTransactionReceiptProcessor
import sttp.client3.*
import sttp.model.{MediaType, StatusCode}

import lib.crypto.{CryptoOps, KeyPair}
import lib.crypto.Hash.ops.*
import lib.crypto.Sign.ops.*
import lib.datatype.*
import api.model.*
import api.model.TransactionWithResult.ops.*
import api.model.api_model.{AccountInfo, BalanceInfo, NftBalanceInfo}
import api.model.token.*

object EthGatewayWithdrawMain extends IOApp:

  given bodyJsonSerializer[A: Encoder]: BodySerializer[A] =
    (a: A) =>
      val serialized = a.asJson.noSpaces
      StringBody(serialized, "UTF-8", MediaType.ApplicationJson)

  val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()

  val getConfig: IO[Config] = IO.blocking(ConfigFactory.load)

  case class GatewayConf(
      ethAddress: String,
      ethChainId: Int,
      ethContract: String,
      ethNftContract: String,
      ethPrivate: String,
      lmPrivate: String,
      lmAddress: String,
      gatewayEthAddress: String,
  )

  object GatewayConf:
    def fromConfig(config: Config): GatewayConf =
      GatewayConf(
        ethAddress = config.getString("eth-address"),
        ethChainId = config.getInt("eth-chain-id"),
        ethContract = config.getString("eth-contract"),
        ethNftContract = config.getString("eth-nft-contract"),
        ethPrivate = config.getString("eth-private"),
        lmPrivate = config.getString("lm-private"),
        lmAddress = config.getString("lm-address"),
        gatewayEthAddress = config.getString("gateway-eth-address"),
      )

  def web3Resource(url: String): Resource[IO, Web3j] = Resource.make {

    val interceptor = HttpLoggingInterceptor()
    interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

    val client = OkHttpClient
      .Builder()
      .addInterceptor(interceptor)
      .build()

    IO(Web3j.build(new HttpService(url, client)))
  }(web3j => IO(web3j.shutdown()))

  def submitTx(
      lmAddress: String,
      account: Account,
      keyPair: KeyPair,
      tx: Transaction,
  ): IO[Unit] =
    IO.blocking {
      val Right(sig) = keyPair.sign(tx): @unchecked
      val signedTxs  = Seq(Signed(AccountSignature(sig, account), tx))

      scribe.info(s"Sending signed transactions: $signedTxs")

      val response = basicRequest
        .response(asStringAlways)
        .post(uri"http://$lmAddress/tx")
        .body(signedTxs)
        .send(backend)

      scribe.info(s"Response: $response")
    }

  def checkLoop(
      web3j: Web3j,
      ethChainId: Int,
      lmAddress: String,
      ethContract: String,
      gatewayEthAddress: String,
      ethPrivate: String,
      keyPair: KeyPair,
  ): IO[Unit] =
    def run: IO[Unit] = for
      _ <- IO.delay(scribe.info(s"Withdrawal check started"))
      _ <- checkLmWithdrawal(
        web3j,
        ethChainId,
        lmAddress,
        ethContract,
        gatewayEthAddress,
        ethPrivate,
        keyPair,
      )
      _ <- checkNftWithdrawal(
        web3j,
        ethChainId,
        lmAddress,
        ethContract,
        gatewayEthAddress,
        ethPrivate,
        keyPair,
      )
      _ <- IO.delay(scribe.info(s"Withdrawal check finished"))
    yield ()

    def loop: IO[Unit] = for
      _ <- run.orElse(IO.unit)
      _ <- IO.sleep(10000.millis)
      _ <- loop
    yield ()

    loop

  def getFungibleBalance(
      lmAddress: String,
  ): IO[Map[TokenDefinitionId, BalanceInfo]] = IO {
    val response = basicRequest
      .response(asStringAlways)
      .get(uri"http://$lmAddress/balance/eth-gateway?movable=free")
      .send(backend)

    if response.code.isSuccess then
      decode[Map[TokenDefinitionId, BalanceInfo]](response.body) match
        case Right(balanceInfoMap) => balanceInfoMap
        case Left(error) =>
          scribe.error(s"Error decoding balance info: $error")
          scribe.error(s"response: ${response.body}")
          Map.empty
    else if response.code.code === StatusCode.NotFound.code then
      scribe.info(
        s"balance of account eth-gateway not found: ${response.body}",
      )
      Map.empty
    else
      scribe.error(s"Error getting balance: ${response.body}")
      Map.empty
  }

  def getNftBalance(
      lmAddress: String,
  ): IO[Map[TokenId, NftBalanceInfo]] = IO {
    val response = basicRequest
      .response(asStringAlways)
      .get(uri"http://$lmAddress/nft-balance/eth-gateway?movable=free")
      .send(backend)

    if response.code.isSuccess then
      decode[Map[TokenId, NftBalanceInfo]](response.body) match
        case Right(balanceInfoMap) => balanceInfoMap
        case Left(error) =>
          scribe.error(s"Error decoding nft-balance info: $error")
          scribe.error(s"response: ${response.body}")
          Map.empty
    else if response.code.code === StatusCode.NotFound.code then
      scribe.info(
        s"nft-balance of account eth-gateway not found: ${response.body}",
      )
      Map.empty
    else
      scribe.error(s"Error getting nft-balance: ${response.body}")
      Map.empty
  }

  def getAccountInfo(
      lmAddress: String,
      account: Account,
  ): IO[Option[AccountInfo]] = IO {
    val response = basicRequest
      .response(asStringAlways)
      .get(uri"http://$lmAddress/account/${account.utf8.value}")
      .send(backend)

    if response.code.isSuccess then
      decode[AccountInfo](response.body) match
        case Right(accountInfo) => Some(accountInfo)
        case Left(error) =>
          scribe.error(s"Error decoding account info: $error")
          None
    else if response.code.code === StatusCode.NotFound.code then
      scribe.info(s"account info not found: ${response.body}")
      None
    else
      scribe.error(s"Error getting account info: ${response.body}")
      None
  }

  def checkLmWithdrawal(
      web3j: Web3j,
      ethChainId: Int,
      lmAddress: String,
      ethLmContract: String,
      gatewayEthAddress: String,
      ethPrivate: String,
      keyPair: KeyPair,
  ): IO[Unit] = getFungibleBalance(lmAddress)
    .flatMap { (balanceMap: Map[TokenDefinitionId, BalanceInfo]) =>

      val gatewayAccount = Account(Utf8.unsafeFrom("eth-gateway"))
      val LM = TokenDefinitionId(Utf8.unsafeFrom("LM"))

      balanceMap
        .get(LM)
        .toSeq
        .flatMap(_.unused.toSeq)
        .filterNot(_._2.signedTx.sig.account === gatewayAccount)
        .traverse { case (txHash, txWithResult) =>
          txWithResult.signedTx.value match
            case tx: Transaction.TokenTx.TransferFungibleToken =>
              {
                for
                  amount <- OptionT.fromOption[IO](
                    tx.outputs.get(gatewayAccount),
                  )
                  accountInfo <- OptionT(
                    getAccountInfo(lmAddress, txWithResult.signedTx.sig.account),
                  )
                  ethAddress <- OptionT.fromOption[IO](accountInfo.ethAddress)
                  _ <- OptionT.liftF {
                    transferEthLM(
                      web3j = web3j,
                      ethChainId = ethChainId,
                      ethLmContract = ethLmContract,
                      gatewayEthAddress = gatewayEthAddress,
                      ethPrivate = ethPrivate,
                      keyPair = keyPair,
                      receiverEthAddress = ethAddress.utf8.value,
                      amount = amount,
                    )
                  }
                  tx1 = Transaction.TokenTx.TransferFungibleToken(
                    networkId = NetworkId(BigNat.unsafeFromLong(1000L)),
                    createdAt = Instant.now(),
                    tokenDefinitionId = LM,
                    inputs = Set(txHash.toSignedTxHash),
                    outputs = Map(gatewayAccount -> amount),
                    memo = Some(Utf8.unsafeFrom{
                      s"After withdrawing of ${txWithResult.signedTx.sig.account}'s $amount"
                    })
                  )
                  _ <- OptionT.liftF(
                    submitTx(lmAddress, gatewayAccount, keyPair, tx1),
                  )
                yield ()
              }.value
            case _ => IO.unit
        }
    }
    .as(())

  def checkNftWithdrawal(
      web3j: Web3j,
      ethChainId: Int,
      lmAddress: String,
      ethNftContract: String,
      gatewayEthAddress: String,
      ethPrivate: String,
      keyPair: KeyPair,
  ): IO[Unit] = getNftBalance(lmAddress)
    .flatMap { (balanceMap: Map[TokenId, NftBalanceInfo]) =>

      val gatewayAccount = Account(Utf8.unsafeFrom("eth-gateway"))

      balanceMap.toSeq.traverse { case (tokenId, balanceInfo) =>
        balanceInfo.tx.signedTx.value match
          case tx: Transaction.TokenTx.TransferNFT
              if balanceInfo.tx.signedTx.sig.account != gatewayAccount =>
            {
              for
                info <- OptionT(
                  getAccountInfo(lmAddress, balanceInfo.tx.signedTx.sig.account),
                )
                ethAddress <- OptionT.fromOption[IO](info.ethAddress)
                _ <- OptionT.liftF {
                  mintEthNft(
                    web3j = web3j,
                    ethChainId = ethChainId,
                    ethNftContract = ethNftContract,
                    gatewayEthAddress = gatewayEthAddress,
                    ethPrivate = ethPrivate,
                    keyPair = keyPair,
                    receiverEthAddress = ethAddress.utf8.value,
                    tokenId = tokenId,
                  )
                }
                tx1 = tx.copy(
                  createdAt = Instant.now,
                  input = balanceInfo.tx.signedTx.toHash,
                  output = gatewayAccount,
                  memo =
                    Some(Utf8.unsafeFrom("gateway balance after withdrawal")),
                )
                _ <- OptionT.liftF(
                  submitTx(lmAddress, gatewayAccount, keyPair, tx1),
                )
              yield ()
            }.value
          case _ => IO.unit
      }
    }
    .as(())

  def transferEthLM(
      web3j: Web3j,
      ethChainId: Int,
      ethLmContract: String,
      gatewayEthAddress: String,
      ethPrivate: String,
      keyPair: KeyPair,
      receiverEthAddress: String,
      amount: BigNat,
  ): IO[Unit] = IO.delay {
    val credential = Credentials.create(ethPrivate)
    assert(
      credential.getAddress() === gatewayEthAddress.toLowerCase(),
      s"invalid gateway eth address: ${credential.getAddress} vs $gatewayEthAddress",
    )
    val gasPrice              = BigInteger.valueOf(50000)
    val gasLimit              = DefaultGasProvider.GAS_LIMIT
    val gasProvider           = new StaticGasProvider(gasPrice, gasLimit)
    val TX_END_CHECK_DURATION = 20000
    val TX_END_CHECK_RETRY    = 9
    val receiptProcessor = new PollingTransactionReceiptProcessor(
      web3j,
      TX_END_CHECK_DURATION,
      TX_END_CHECK_RETRY,
    )
    val manager =
      new RawTransactionManager(web3j, credential, ethChainId, receiptProcessor)

    val contract = ERC20.load(ethLmContract, web3j, manager, gasProvider)

    val mintParams = new ArrayList[Type[?]]()
    mintParams.add(new Address(receiverEthAddress))
    mintParams.add(new Uint256(amount.toBigInt.bigInteger))

    val returnTypes = Collections.emptyList[TypeReference[?]]()

    val transferTxData = FunctionEncoder.encode {
      new Function("transfer", mintParams, returnTypes)
    }

    val mintRes = manager
      .sendEIP1559Transaction(
        ethChainId,
        BigInteger.valueOf(50000),
        BigInteger.valueOf(50000),
        DefaultGasProvider.GAS_LIMIT,
        ethLmContract,
        transferTxData,
        BigInteger.ZERO,
        false,
      )
      .getResult()

    scribe.info("mintRes: " + mintRes)
  }

  def mintEthNft(
      web3j: Web3j,
      ethChainId: Int,
      ethNftContract: String,
      gatewayEthAddress: String,
      ethPrivate: String,
      keyPair: KeyPair,
      receiverEthAddress: String,
      tokenId: TokenId,
  ): IO[Unit] = IO.delay {
    val credential = Credentials.create(ethPrivate)
    assert(
      credential.getAddress() === gatewayEthAddress.toLowerCase(),
      s"invalid gateway eth address: ${credential.getAddress} vs $gatewayEthAddress",
    )
    val gasPrice              = BigInteger.valueOf(50000)
    val gasLimit              = DefaultGasProvider.GAS_LIMIT
    val gasProvider           = new StaticGasProvider(gasPrice, gasLimit)
    val TX_END_CHECK_DURATION = 20000
    val TX_END_CHECK_RETRY    = 9
    val receiptProcessor = new PollingTransactionReceiptProcessor(
      web3j,
      TX_END_CHECK_DURATION,
      TX_END_CHECK_RETRY,
    )
    val manager =
      new RawTransactionManager(web3j, credential, ethChainId, receiptProcessor)

    val contract = ERC721.load(ethNftContract, web3j, manager, gasProvider)

    val tokenIdBigInt = BigInt(tokenId.utf8.value)

    val mintParams = new ArrayList[Type[?]]()
    mintParams.add(new Address(receiverEthAddress))
    mintParams.add(new Uint256(tokenIdBigInt.bigInteger))

    val returnTypes = Collections.emptyList[TypeReference[?]]()

    val mintTxData = FunctionEncoder.encode {
      new Function("safeMint", mintParams, returnTypes)
    }

    val mintRes = manager
      .sendEIP1559Transaction(
        ethChainId,
        BigInteger.valueOf(50000),
        BigInteger.valueOf(50000),
        DefaultGasProvider.GAS_LIMIT, // BigInteger.valueOf(50000),
        ethNftContract,
        mintTxData,
        BigInteger.ZERO,
        false,
      )
      .getResult()

    scribe.info("mintRes: " + mintRes)

    scribe.info(
      s"owner of token id ($tokenId) : " + contract
        .ownerOf(tokenIdBigInt.bigInteger)
        .send(),
    )
  }
  def run(args: List[String]): IO[ExitCode] =
    for
      conf <- getConfig
      gatewayConf = GatewayConf.fromConfig(conf)
      keyPair = CryptoOps.fromPrivate(
        BigInt(gatewayConf.ethPrivate.drop(2), 16),
      )
      _ <- web3Resource(gatewayConf.ethAddress).use { web3 =>
        checkLoop(
          web3j = web3,
          ethChainId = gatewayConf.ethChainId,
          lmAddress = gatewayConf.lmAddress,
          ethContract = gatewayConf.ethContract,
          gatewayEthAddress = gatewayConf.gatewayEthAddress,
          ethPrivate = gatewayConf.ethPrivate,
          keyPair = keyPair,
        )
      }
    yield ExitCode.Success
