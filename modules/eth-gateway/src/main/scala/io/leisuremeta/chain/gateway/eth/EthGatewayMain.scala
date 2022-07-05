package io.leisuremeta.chain
package gateway.eth

import java.math.BigInteger
import java.util.{Arrays, ArrayList, Collections}
import java.time.Instant
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import scala.concurrent.duration.*

import cats.effect.{ExitCode, IO, IOApp, OutcomeIO, Resource}
import cats.syntax.eq.*
import cats.syntax.traverse.*

import com.typesafe.config.{Config, ConfigFactory}
import io.circe.Encoder
import io.circe.syntax.given
import org.web3j.abi.{
  EventEncoder,
  FunctionEncoder,
  FunctionReturnDecoder,
  TypeReference,
}
import org.web3j.abi.datatypes.{Address, Event, Function, Type}
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.{Credentials, MnemonicUtils}
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.{
  DefaultBlockParameter,
  DefaultBlockParameterName,
}
import org.web3j.protocol.core.methods.request.{EthFilter}
import org.web3j.protocol.core.methods.response.EthLog.{LogResult, LogObject}
import org.web3j.protocol.http.HttpService
import sttp.client3.*
import sttp.model.{MediaType, StatusCode}

import lib.crypto.{CryptoOps, KeyPair}
import lib.crypto.Hash.ops.*
import lib.crypto.Sign.ops.*
import lib.datatype.*
import api.model.*
import api.model.token.*

object EthGatewayMain extends IOApp:

  given bodyJsonSerializer[A: Encoder]: BodySerializer[A] =
    (a: A) =>
      val serialized = a.asJson.noSpaces
      StringBody(serialized, "UTF-8", MediaType.ApplicationJson)

  val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()

  val getConfig: IO[Config] = IO.blocking(ConfigFactory.load)

  case class GatewayConf(
      ethAddress: String,
      ethContract: String,
      ethPrivate: String,
      lmPrivate: String,
      lmAddress: String,
      gatewayEthAddress: String,
  )
  object GatewayConf:
    def fromConfig(config: Config): GatewayConf =
      GatewayConf(
        ethAddress = config.getString("eth-address"),
        ethContract = config.getString("eth-contract"),
        ethPrivate = config.getString("eth-private"),
        lmPrivate = config.getString("lm-private"),
        lmAddress = config.getString("lm-address"),
        gatewayEthAddress = config.getString("gateway-eth-address"),
      )

  def web3Resource(url: String): Resource[IO, Web3j] = Resource.make {
    IO(Web3j.build(new HttpService(url)))
  }(web3j => IO(web3j.shutdown()))

  case class TransferTokenEvent(
      blockNumber: BigInt,
      txHash: String,
      from: String,
      to: String,
      value: BigInt,
  )

  extension [A](logResult: LogResult[A])
    def toTransferTokenEvent: TransferTokenEvent =

      val log = logResult.get.asInstanceOf[LogObject].get

      val typeRefAddress: TypeReference[Address] =
        new TypeReference[Address]() {}
      val typeRefUint256: TypeReference[Type[?]] =
        (new TypeReference[Uint256]() {}).asInstanceOf[TypeReference[Type[?]]]

      val topics = log.getTopics.asScala
      val from: Address = FunctionReturnDecoder
        .decodeIndexedValue[Address](topics(1), typeRefAddress)
        .asInstanceOf[Address]
      val to: Address = FunctionReturnDecoder
        .decodeIndexedValue[Address](topics(2), typeRefAddress)
        .asInstanceOf[Address]
      val amount = FunctionReturnDecoder
        .decode(log.getData, List(typeRefUint256).asJava)
        .asScala
        .head
        .asInstanceOf[Uint256]
        .getValue

      TransferTokenEvent(
        blockNumber = BigInt(log.getBlockNumber),
        txHash = log.getTransactionHash,
        from = from.getValue,
        to = to.getValue,
        value = BigInt(amount),
      )

  def getEthBlockNumber(web3j: Web3j): IO[BigInt] =
    IO.fromCompletableFuture(IO(web3j.ethBlockNumber.sendAsync))
      .map(_.getBlockNumber)
      .map(BigInt(_))

  def getTransferTokenEvents(
      web3j: Web3j,
      contractAddress: String,
      fromBlock: DefaultBlockParameter,
      toBlock: DefaultBlockParameter,
  ): IO[Seq[TransferTokenEvent]] =
    val TransferEvent = new Event(
      "Transfer",
      Arrays.asList(
        new TypeReference[Address]() {},
        new TypeReference[Address]() {},
        new TypeReference[Uint256]() {},
      ),
    )

    val filter = new EthFilter(fromBlock, toBlock, contractAddress)
    filter.addSingleTopic(EventEncoder.encode(TransferEvent))

    IO.fromCompletableFuture(IO(web3j.ethGetLogs(filter).sendAsync())).map {
      ethLog => ethLog.getLogs.asScala.map(_.toTransferTokenEvent).toSeq
    }

  def transferToken(
      web3j: Web3j,
      privateKey: String,
      contractAddress: String,
      toAddress: String,
      amount: BigInt,
  ): IO[String] =
    val credentials = Credentials.create(privateKey)

    val params = new ArrayList[Type[?]]()
    params.add(new Address(toAddress))
    params.add(new Uint256(amount.bigInteger))

    val returnTypes = Collections.emptyList[TypeReference[?]]()

    val function = new Function(
      "transfer",
      params,
      returnTypes,
    )

    val txData = FunctionEncoder.encode(function)

    ???

  def submitTx(
      lmAddress: String,
      account: Account,
      keyPair: KeyPair,
      tx: Transaction,
  ) =
    val Right(sig) = keyPair.sign(tx)
    val signedTxs  = Seq(Signed(AccountSignature(sig, account), tx))

    scribe.info(s"Sending signed transactions: $signedTxs")

    val response = basicRequest
      .response(asStringAlways)
      .post(uri"http://$lmAddress/tx")
      .body(signedTxs)
      .send(backend)

    scribe.info(s"Response: $response")

  def initializeLmChain(lmAddress: String, keyPair: KeyPair): IO[Unit] = IO {

    val networkId = NetworkId(BigNat.unsafeFromLong(1000L))
    val account   = Account(Utf8.unsafeFrom("eth-gateway"))

    val createAccount: Transaction = Transaction.AccountTx.CreateAccount(
      networkId = networkId,
      createdAt = Instant.now(),
      account = account,
      ethAddress = None,
      guardian = None,
    )

    submitTx(lmAddress, account, keyPair, createAccount)

//    val createGroup: Transaction = Transaction.GroupTx.CreateGroup(
//      networkId = networkId,
//      createdAt = Instant.now(),
//      groupId = GroupId(Utf8.unsafeFrom("lm-minter-group")),
//      name = Utf8.unsafeFrom("lm-minter-group"),
//      coordinator = account,
//    )
//
//    submitTx(lmAddress, account, keyPair, createGroup)
//
//    val addAccounts: Transaction = Transaction.GroupTx.AddAccounts(
//      networkId = networkId,
//      createdAt = Instant.now(),
//      groupId = GroupId(Utf8.unsafeFrom("lm-minter-group")),
//      accounts = Set(account),
//    )
//
//    submitTx(lmAddress, account, keyPair, addAccounts)

    ()
  }

  def mintLM(
      lmAddress: String,
      keyPair: KeyPair,
      toAccount: Account,
      amount: BigInt,
  ): IO[Unit] = IO {

    val networkId = NetworkId(BigNat.unsafeFromLong(1000L))
    val lmDef     = TokenDefinitionId(Utf8.unsafeFrom("LM"))
    val account   = Account(Utf8.unsafeFrom("eth-gateway"))

    val mintFungibleToken = Transaction.TokenTx.MintFungibleToken(
      networkId = networkId,
      createdAt = Instant.now(),
      definitionId = lmDef,
      outputs = Map(toAccount -> BigNat.unsafeFromBigInt(amount)),
    )

    submitTx(lmAddress, account, keyPair, mintFungibleToken)

    ()
  }

  def findAccountByEthAddress(
      lmAddress: String,
      ethAddress: String,
  ): IO[Option[Account]] = IO {
    val response = basicRequest
      .response(asStringAlways)
      .get(uri"http://$lmAddress/eth/$ethAddress")
      .send(backend)

    if response.code.isSuccess then
      Some(Account(Utf8.unsafeFrom(response.body.drop(1).dropRight(1))))
    else
      scribe.info(s"Account $ethAddress not found: ${response.body}")
      None
  }

  def checkDepositAndMint(
      web3j: Web3j,
      lmAddress: String,
      ethContract: String,
      gatewayEthAddress: String,
      keyPair: KeyPair,
      startBlockNumber: BigInt,
      endBlockNumber: BigInt,
  ): IO[Unit] =
    for
      events <- getTransferTokenEvents(
        web3j,
        ethContract,
        DefaultBlockParameter.valueOf(startBlockNumber.bigInteger),
        DefaultBlockParameter.valueOf(endBlockNumber.bigInteger),
      )
      _ <- IO.delay(scribe.info(s"events: $events"))
      depositEvents = events.filter(
        _.to.toLowerCase === gatewayEthAddress.toLowerCase,
      )
      toMints <- depositEvents.toList.traverse { event =>
        val amount    = event.value
        val toAccount = Account(Utf8.unsafeFrom(event.to))
        findAccountByEthAddress(lmAddress, event.from).map {
          (accountOption: Option[Account]) =>
            accountOption.map { account =>
              (account, amount)
            }
        }
      }
      _ <- IO.delay(scribe.info(s"deposit events: $depositEvents"))
      _ <- IO.delay(scribe.info(s"toMints: $toMints"))
      _ <- toMints.flatten.traverse { case (account, amount) =>
        mintLM(lmAddress, keyPair, account, amount)
      }
    yield ()

  def depositLoop(
      web3j: Web3j,
      lmAddress: String,
      ethContract: String,
      gatewayEthAddress: String,
      keyPair: KeyPair,
      startBlockNumber: BigInt,
  ): IO[Unit] =
    def run(startBlockNumber: BigInt): IO[BigInt] = for
      _ <- IO.delay(scribe.info(s"Checking for deposit events"))
      blockNumber <- IO
        .fromCompletableFuture(IO(web3j.ethBlockNumber.sendAsync()))
        .map(_.getBlockNumber)
        .map(BigInt(_))
      _ <- IO.delay(scribe.info(s"blockNumber: $blockNumber"))
      endBlockNumber = blockNumber - 6
      _ <- IO.delay(
        scribe.info(s"startBlockNumber: $startBlockNumber"),
        scribe.info(
          s"blockNumber: $blockNumber, endBlockNumber: $endBlockNumber",
        ),
      )
      _ <- checkDepositAndMint(
        web3j,
        lmAddress,
        ethContract,
        gatewayEthAddress,
        keyPair,
        startBlockNumber,
        endBlockNumber,
      )
      _ <- IO.delay(scribe.info(s"Deposit resource finished"))
      _ <- IO.sleep(1000.millis)
    yield endBlockNumber

    def loop(startBlockNumber: BigInt): IO[Unit] = for
      blockNumber <- run(startBlockNumber)
      _           <- loop(blockNumber + 1)
    yield ()

    loop(startBlockNumber)

  def getGasPrice(weg3j: Web3j): IO[BigInt] =
    IO.fromCompletableFuture(IO(weg3j.ethGasPrice.sendAsync())).map {
      ethGasPrice => BigInt(ethGasPrice.getGasPrice)
    }

  def run(args: List[String]): IO[ExitCode] =

    val from = DefaultBlockParameterName.EARLIEST
    val to   = DefaultBlockParameterName.LATEST

    for
      conf <- getConfig
      gatewayConf = GatewayConf.fromConfig(conf)
      keyPair = CryptoOps.fromPrivate(BigInt(gatewayConf.ethPrivate.drop(2), 16))
      _ <- web3Resource(gatewayConf.ethAddress).use { web3 =>
        depositLoop(
          web3j = web3,
          lmAddress = gatewayConf.lmAddress,
          ethContract = gatewayConf.ethContract,
          gatewayEthAddress = gatewayConf.gatewayEthAddress,
          keyPair = keyPair,
          startBlockNumber = BigInt(0),
        )

//        for
//          gasPrice <- getGasPrice(web3)
//        yield
//          println(s"gasPrice: $gasPrice")
      }
    yield ExitCode.Success
