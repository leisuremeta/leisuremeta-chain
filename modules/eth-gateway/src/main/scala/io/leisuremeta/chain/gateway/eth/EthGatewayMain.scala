package io.leisuremeta.chain
package gateway.eth

import java.math.BigInteger
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.util.{Arrays, ArrayList, Collections}
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import scala.concurrent.duration.*
import scala.util.Try

import cats.effect.{ExitCode, IO, IOApp, OutcomeIO, Resource}
import cats.syntax.applicativeError.*
import cats.syntax.bifunctor.*
import cats.syntax.eq.*
import cats.syntax.traverse.*

import com.github.jasync.sql.db.{Connection, QueryResult}
import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.Encoder
import io.circe.syntax.given
import io.circe.generic.auto.*
import io.circe.parser.decode
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
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.response.PollingTransactionReceiptProcessor
import sttp.client3.*
import sttp.model.{MediaType, StatusCode}

import lib.crypto.{CryptoOps, KeyPair}
import lib.crypto.Hash.ops.*
import lib.crypto.Sign.ops.*
import lib.datatype.*
import api.model.*
import api.model.api_model.{AccountInfo, BalanceInfo}
import api.model.token.*
import api.model.TransactionWithResult.ops.toSignedTxHash
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.OkHttpClient

object EthGatewayMain extends IOApp:

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
      ethPrivate: String,
      lmPrivate: String,
      lmAddress: String,
      gatewayEthAddress: String,
      mysqlHost: String,
      mysqlPort: Int,
      mysqlDatabase: String,
      mysqlUsername: String,
      mysqlPassword: String,
  )
  object GatewayConf:
    def fromConfig(config: Config): GatewayConf =
      GatewayConf(
        ethAddress = config.getString("eth-address"),
        ethChainId = config.getInt("eth-chain-id"),
        ethContract = config.getString("eth-contract"),
        ethPrivate = config.getString("eth-private"),
        lmPrivate = config.getString("lm-private"),
        lmAddress = config.getString("lm-address"),
        gatewayEthAddress = config.getString("gateway-eth-address"),
        mysqlHost = config.getString("mysql-host"),
        mysqlPort = config.getInt("mysql-port"),
        mysqlDatabase = config.getString("mysql-database"),
        mysqlUsername = config.getString("mysql-username"),
        mysqlPassword = config.getString("mysql-password"),
      )

  def mysqlResource(config: GatewayConf): Resource[IO, Connection] =
    Resource.make(IO {
      MySQLConnectionBuilder.createConnectionPool(
        s"jdbc:mysql://${config.mysqlHost}:${config.mysqlPort}/${config.mysqlDatabase}?user=${config.mysqlUsername}&password=${config.mysqlPassword}",
      )
    })(connection =>
      IO.fromCompletableFuture(IO(connection.disconnect())).map(_ => ()),
    )

  def web3Resource(url: String): Resource[IO, Web3j] = Resource.make {

    val interceptor = HttpLoggingInterceptor()
    interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

    val client = OkHttpClient
      .Builder()
      //     .addInterceptor(interceptor)
      .build()

    IO(Web3j.build(new HttpService(url, client)))
  }(web3j => IO(web3j.shutdown()))

  def allResource(
      config: GatewayConf,
      url: String,
  ): Resource[IO, (Connection, Web3j)] =
    for
      conn  <- mysqlResource(config)
      web3j <- web3Resource(url)
    yield (conn, web3j)

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

  def writeUnsentDeposits(deposits: Seq[TransferTokenEvent]): IO[Unit] =
    IO.blocking {
      val path = Paths.get("unsent-deposits.json")
      val json = deposits.asJson.spaces2
      Files.write(
        path,
        json.getBytes,
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING,
      )
    }

  def readUnsentDeposits(): IO[Seq[TransferTokenEvent]] = IO.blocking {
    val path = Paths.get("unsent-deposits.json")
    val seqEither = for
      json <- Try(Files.readAllLines(path).asScala.mkString("\n")).toEither
      seq  <- decode[Seq[TransferTokenEvent]](json)
    yield seq
    seqEither match
      case Right(seq) => seq
      case Left(e) =>
        e.printStackTrace()
        scribe.error(s"Error reading unsent deposits: ${e.getMessage}")
        Seq.empty
  }

  def logSentDeposits(deposits: Seq[(Account, TransferTokenEvent)]): IO[Unit] =
    if deposits.isEmpty then IO.unit
    else
      IO.blocking {
        val path = Paths.get("sent-deposits.logs")
        val jsons =
          deposits.toSeq.map(_.asJson.noSpaces).mkString("", "\n", "\n")
        Files.write(
          path,
          jsons.getBytes,
          StandardOpenOption.CREATE,
          StandardOpenOption.WRITE,
          StandardOpenOption.APPEND,
        )
      }

  def writeLastBlockRead(blockNumber: BigInt): IO[Unit] = IO.blocking {
    val path = Paths.get("last-block-read.json")
    val json = blockNumber.asJson.spaces2
    Files.write(
      path,
      json.getBytes,
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.TRUNCATE_EXISTING,
    )
  }

  def readLastBlockRead(): IO[BigInt] = IO.blocking {
    val path = Paths.get("last-block-read.json")
    val blockNumberEither = for
      json <- Try(Files.readAllLines(path).asScala.mkString("\n")).toEither
      blockNumber <- decode[BigInt](json)
    yield blockNumber
    blockNumberEither match
      case Right(blockNumber) => blockNumber
      case Left(e) =>
        scribe.error(s"Error reading last block read: ${e.getMessage}")
        BigInt(0)
  }

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

  def getEthBlockTime(web3j: Web3j)(blockNumber: BigInt): IO[Instant] =
    for ethBlock <- IO.fromCompletableFuture(IO.delay {
        web3j
          .ethGetBlockByNumber(
            DefaultBlockParameter.valueOf(blockNumber.bigInteger),
            false,
          )
          .sendAsync()
      })
    yield Instant.ofEpochSecond(ethBlock.getBlock().getTimestamp().longValue())

  def transferToken(
      web3j: Web3j,
      ethChainId: Int,
      privateKey: String,
      contractAddress: String,
      toAddress: String,
      amount: BigInt,
      gasPrice: BigInt,
  ): IO[Either[String, TransactionReceipt]] = IO {

    scribe.info(
      s"transferToken: $contractAddress, $toAddress, $amount, $gasPrice",
    )

    val credential = Credentials.create(privateKey)

    val params = new ArrayList[Type[?]]()
    params.add(new Address(toAddress))
    params.add(new Uint256(amount.bigInteger))

    // val returnTypes = Collections.singletonList[TypeReference[?]](new TypeReference[Type[?]](){})
    val returnTypes = Collections.emptyList[TypeReference[?]]()

    val function = new Function(
      "transfer",
      params,
      returnTypes,
    )

    val txData = FunctionEncoder.encode(function)

    scribe.debug(s"txData: $txData")

    val TX_END_CHECK_DURATION = 20000
    val TX_END_CHECK_RETRY    = 9
    val receiptProcessor = new PollingTransactionReceiptProcessor(
      web3j,
      TX_END_CHECK_DURATION,
      TX_END_CHECK_RETRY,
    );
    val manager =
      new RawTransactionManager(web3j, credential, ethChainId, receiptProcessor)

    val txHash = manager
      .sendTransaction(
        gasPrice.bigInteger,
        BigInteger.valueOf(1_000_000),
        contractAddress,
        txData,
        BigInteger.ZERO,
      )
      .getTransactionHash

    val receiptEither = Try {
      receiptProcessor.waitForTransactionReceipt(txHash)
    }.toEither.leftMap(_.getMessage)

    scribe.info(s"Receipt: $receiptEither")

    receiptEither
  }

  def submitTx(
      lmAddress: String,
      account: Account,
      keyPair: KeyPair,
      tx: Transaction,
  ) =
    val Right(sig) = keyPair.sign(tx): @unchecked
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
  ): IO[Option[Account]] =
    findAccountByEthAddress0(lmAddress, ethAddress).flatMap{
      accountOption =>
        if accountOption.isEmpty && ethAddress.startsWith("0x") then
          findAccountByEthAddress0(lmAddress, ethAddress.drop(2))
        else
          IO.pure(accountOption)
    }

  def findAccountByEthAddress0(
      lmAddress: String,
      ethAddress: String,
  ): IO[Option[Account]] = IO {
    scribe.info(s"requesting eth address $ethAddress 's LM account")
    try {
      val response = basicRequest
        .response(asStringAlways)
        .get(uri"http://$lmAddress/eth/$ethAddress")
        .send(backend)

      scribe.info(s"eth address $ethAddress response: $response")

      if response.code.isSuccess then
        Some(Account(Utf8.unsafeFrom(response.body.drop(1).dropRight(1))))
      else
        scribe.info(s"Account $ethAddress not found: ${response.body}")
        None
    } catch {
      case (e: Exception) =>
        e.printStackTrace()
        None
    }
  }

  def checkDepositAndMint(
      web3j: Web3j,
      mysqlConnection: Connection,
      mysqlDatabase: String,
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
      _ <- IO.delay(scribe.info(s"current deposit events: $depositEvents"))
      oldEvents <- readUnsentDeposits()
      _         <- IO.delay(scribe.info(s"old deposit events: $oldEvents"))
      allEvents = depositEvents ++ oldEvents
      _ <- IO.delay(scribe.info(s"all deposit events: $allEvents"))
      eventAndAccountOptions <- allEvents.toList.traverse { event =>
//        scribe.info(s"current event: $event")
        val amount    = event.value
        val toAccount = Account(Utf8.unsafeFrom(event.to))
        findAccountByEthAddress(lmAddress, event.from).map {
          (accountOption: Option[Account]) =>
            scribe.info(s"eth address ${event.from}'s LM account: $accountOption")
            (event, accountOption)
        }
      }
      (known, unknown) = eventAndAccountOptions.partition(_._2.nonEmpty)
      toMints = known.map {
        case (event, Some(account)) => (account, event)
        case (event, None) =>
          throw new Exception(
            s"Internal error: Account ${event.from} not found",
          )
      }
      _ <- IO.delay(scribe.info(s"toMints: $toMints"))
      _ <- toMints.traverse { case (account, event) =>
        mintLM(lmAddress, keyPair, account, event.value)
      }
      _ <- logSentDeposits(toMints.map(_._1).zip(known.map(_._1)))
      _ <- toMints.traverse { case (account, event) =>
        logToMysql(mysqlConnection)(
          mysqlDatabase = mysqlDatabase,
          ethTxHash = event.txHash,
          timestamp = Instant.now(),
          userEthAccount = "0xFcd1853d09F7Df77f17003B69dDc78b3f8bD5D0f",
          userLmcAccount = "acf8526119abe74fb9cb371ce480fba8009cbaea",
          amount = BigInt("1") * BigInt(10).pow(16),
        ).map { queryResult =>
          println(s"Result: $queryResult")
        }
      }
      unsent = unknown.map(_._1)
      _ <- IO.delay(scribe.info(s"unsent: $unsent"))
      _ <- writeUnsentDeposits(unsent)
    yield ()

  def checkLoop(
      web3j: Web3j,
      ethChainId: Int,
      lmAddress: String,
      ethContract: String,
      gatewayEthAddress: String,
      keyPair: KeyPair,
      mysqlConnection: Connection,
      mysqlDatabase: String,
  ): IO[Unit] =
    def run: IO[BigInt] = for
      _ <- IO.delay(scribe.info(s"Checking for deposit / withdrawal events"))
      lastBlockNumber <- readLastBlockRead()
      startBlockNumber = lastBlockNumber + 1
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
        mysqlConnection,
        mysqlDatabase,
        lmAddress,
        ethContract,
        gatewayEthAddress,
        keyPair,
        startBlockNumber,
        endBlockNumber,
      )
      _ <- writeLastBlockRead(endBlockNumber)
      _ <- IO.delay(scribe.info(s"Deposit check finished."))
//      _ <- IO.delay(scribe.info(s"Withdrawal check started"))
//      _ <- checkWithdrawal(
//        web3j,
//        ethChainId,
//        lmAddress,
//        ethContract,
//        gatewayEthAddress,
//        keyPair,
//      )
//      _ <- IO.delay(scribe.info(s"Withdrawal check finished"))
    yield endBlockNumber

    def loop: IO[Unit] = for
      _ <- run.orElse(IO.unit)
      _ <- IO.sleep(10000.millis)
      _ <- loop
    yield ()

    loop

  def getGasPrice(weg3j: Web3j): IO[BigInt] =
    IO.fromCompletableFuture(IO(weg3j.ethGasPrice.sendAsync())).map {
      ethGasPrice => BigInt(ethGasPrice.getGasPrice)
    }

  def getBalance(
      lmAddress: String,
  ): IO[Option[BalanceInfo]] = IO {
    val lmDef = TokenDefinitionId(Utf8.unsafeFrom("LM"))
    val response = basicRequest
      .response(asStringAlways)
      .get(uri"http://$lmAddress/balance/eth-gateway?movable=all")
      .send(backend)

    if response.code.isSuccess then
      decode[Map[TokenDefinitionId, BalanceInfo]](response.body) match
        case Right(balanceInfoMap) => balanceInfoMap.get(lmDef)
        case Left(error) =>
          scribe.error(s"Error decoding balance info: $error")
          scribe.error(s"response: ${response.body}")
          None
    else if response.code.code === StatusCode.NotFound.code then
      scribe.info(s"balance of account eth-gateway not found: ${response.body}")
      None
    else
      scribe.error(s"Error getting balance: ${response.body}")
      None
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

  def checkWithdrawal(
      web3j: Web3j,
      ethChainId: Int,
      lmAddress: String,
      ethContract: String,
      gatewayEthAddress: String,
      keyPair: KeyPair,
  ): IO[Unit] = getBalance(lmAddress).flatMap {
    case None => IO.delay(scribe.info("No balance to withdraw"))
    case Some(balanceInfo) =>
      val networkId      = NetworkId(BigNat.unsafeFromLong(1000L))
      val lmDef          = TokenDefinitionId(Utf8.unsafeFrom("LM"))
      val gatewayAccount = Account(Utf8.unsafeFrom("eth-gateway"))

      val withdrawCandidates = for
        unusedTxHashAndTx <- balanceInfo.unused.toSeq
        (txHash, tx) = unusedTxHashAndTx
        inputAccount = tx.signedTx.sig.account
        item <- tx.signedTx.value match
          case tf: Transaction.TokenTx.TransferFungibleToken
              if inputAccount != gatewayAccount && tf.tokenDefinitionId === lmDef =>
            tf.outputs
              .get(gatewayAccount)
              .map(amount => (inputAccount, amount))
              .toSeq
          case _ => Seq.empty
      yield (txHash, item)

      scribe.info(s"withdrawCandidates: $withdrawCandidates")

      if withdrawCandidates.isEmpty then
        IO.delay(scribe.info("No withdraw candidates"))
      else
        for
          toWithdrawOptionSeq <- withdrawCandidates.traverse {
            case (txHash, (account, amount)) =>
              getAccountInfo(lmAddress, account).map { accountInfoOption =>
                accountInfoOption.flatMap { accountInfo =>
                  accountInfo.ethAddress.map { ethAddress =>
                    (txHash, account, ethAddress, amount)
                  }
                }
              }
          }
          toWithdraw = toWithdrawOptionSeq.flatten
          _        <- IO.delay(scribe.info(s"toWithdraw: $toWithdraw"))
          gasPrice <- getGasPrice(web3j)
          successfulWithdraws <- toWithdraw.traverse {
            case (txHash, account, ethAddress, amount) =>
              transferToken(
                web3j = web3j,
                ethChainId = ethChainId,
                privateKey = keyPair.privateKey.toString(16),
                contractAddress = ethContract,
                toAddress = ethAddress.utf8.value,
                amount = amount.toBigInt,
                gasPrice = gasPrice,
              ).map {
                case Right(receipt) =>
                  scribe.info(s"withdrawal txHash: $txHash")
                  scribe.info(s"withdrawal account: $account")
                  scribe.info(s"withdrawal ethAddress: $ethAddress")
                  scribe.info(s"withdrawal receipt: $receipt")
                  Some((txHash, amount))
                case Left(error) =>
                  scribe.info(s"Error transferring token to account: $account")
                  None
              }
          }
          _ <- IO.delay(scribe.info(s"withdrawal txs are sent"))
          tx = Transaction.TokenTx.TransferFungibleToken(
            networkId = networkId,
            createdAt = Instant.now,
            tokenDefinitionId = lmDef,
            inputs = successfulWithdraws.flatten.map(_._1.toSignedTxHash).toSet,
            outputs = Map(
              gatewayAccount -> successfulWithdraws.flatten
                .map(_._2)
                .foldLeft(BigNat.Zero)(BigNat.add),
            ),
            memo = None,
          )
          _ <- IO.delay(submitTx(lmAddress, gatewayAccount, keyPair, tx))
          _ <- IO.delay(scribe.info(s"withdrawal utxos are removed"))
        yield ()
  }

  def logToMysql(connection: Connection)(
      mysqlDatabase: String,
      ethTxHash: String,
      timestamp: Instant,
      userEthAccount: String,
      userLmcAccount: String,
      amount: BigInt,
  ): IO[QueryResult] = IO
    .fromCompletableFuture(IO {

      val query =
        s"""CALL $mysqlDatabase.SP_INS_CX_TRADE_INFO_MAINNET_DEPOSIT_T(
'$ethTxHash'	-- 체결주소
, '${timestamp.truncatedTo(ChronoUnit.SECONDS).toString}' 		-- 체결일시
, '521ffc8f1e07eef80c634245c6f0551e30c3d5d0'	-- BLC계정 (playnomm) 주소
, '$userEthAccount'		-- 전달해주는 메인넷 지갑 주소(= Metamask LM지갑 주소)
, '$userLmcAccount'	-- 사용자 지갑주소
, '${BigDecimal(amount, 18)}'	-- 입금금액
, 'LM'	-- tokenDefinitionId": "LM"
, 'T' -- T(Trade)
, 'TM01'	-- TM01 [자산][입금][FT이동 - 메인넷에서 입금]
, 'EN'	-- Default('EN')
);"""

      scribe.info(s"Query: $query")

      connection.sendQuery(query)
    })
    .map { queryResult =>
      scribe.info(s"Result: $queryResult")
      queryResult
    }

  def run(args: List[String]): IO[ExitCode] =
    for
      conf <- getConfig
      gatewayConf = GatewayConf.fromConfig(conf)
      keyPair = CryptoOps.fromPrivate(
        BigInt(gatewayConf.lmPrivate, 16),
      )
      _ <- allResource(gatewayConf, gatewayConf.ethAddress).use {
        (connection, web3) =>

//        initializeLmChain(gatewayConf.lmAddress, keyPair)

          getEthBlockTime(web3)(BigInt(15574432)).map { timestamp =>
            println(s"Timestamp: $timestamp")
          }

          checkLoop(
            web3j = web3,
            ethChainId = gatewayConf.ethChainId,
            lmAddress = gatewayConf.lmAddress,
            ethContract = gatewayConf.ethContract,
            gatewayEthAddress = gatewayConf.gatewayEthAddress,
            keyPair = keyPair,
            mysqlConnection = connection,
            mysqlDatabase = gatewayConf.mysqlDatabase,
          )
      }

//      _ <- mysqlResource(gatewayConf).use{ connection =>
//        logToMysql(connection)(
//          ethTxHash = "0xa7e0c93ad25346e2c53b19c3ca29e7d4139741bc5ae233316c5a8dd3fe8ee620",
//          timestamp = Instant.parse(s"2022-09-19T06:05:00Z"),
//          userEthAccount = "0xFcd1853d09F7Df77f17003B69dDc78b3f8bD5D0f",
//          userLmcAccount = "acf8526119abe74fb9cb371ce480fba8009cbaea",
//          amount = BigInt("1") * BigInt(10).pow(16),
//        ).map{ queryResult =>
//          println(s"Result: $queryResult")
//        }
//      }

//      keyPair = CryptoOps.fromPrivate(
//        BigInt(gatewayConf.ethPrivate.drop(2), 16),
//      )
//      _ <- web3Resource(gatewayConf.ethAddress).use { web3 =>

//        initializeLmChain(gatewayConf.lmAddress, keyPair)

//        checkLoop(
//          web3j = web3,
//          ethChainId = gatewayConf.ethChainId,
//          lmAddress = gatewayConf.lmAddress,
//          ethContract = gatewayConf.ethContract,
//          gatewayEthAddress = gatewayConf.gatewayEthAddress,
//          keyPair = keyPair,
//        )

//        val dest = "0xa0d311fdC182Df002C90469098D3a2B6F40E5cDF"
////        val dest = "0xa0d311fdc182df002c90469098d3a2b6f40e5cdf"
//
//        for
//          gasPrice <- getGasPrice(web3)
//          receipt <- transferToken(
//            web3j = web3,
//            ethChainId = gatewayConf.ethChainId,
//            privateKey = gatewayConf.ethPrivate,
//            contractAddress = gatewayConf.ethContract,
//            toAddress = dest,
//            amount = BigInt("1000000000000000000"), //* BigInt(10).pow(18),
//            gasPrice = gasPrice,
//          )
//        yield
//          ()
    yield ExitCode.Success
