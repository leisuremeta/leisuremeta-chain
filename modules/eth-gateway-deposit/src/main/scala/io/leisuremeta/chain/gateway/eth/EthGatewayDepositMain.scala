package io.leisuremeta.chain
package gateway.eth

//import java.math.BigInteger
import java.nio.charset.StandardCharsets 
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.util.{Arrays, Locale}
import java.time.Instant
//import java.time.temporal.ChronoUnit
import scala.jdk.CollectionConverters.*
//import scala.jdk.FutureConverters.*
import scala.concurrent.duration.*
import scala.util.Try

import cats.data.EitherT
import cats.effect.{Async, Clock, ExitCode, IO, IOApp, Resource}
import cats.syntax.applicativeError.*
import cats.syntax.apply.*
//import cats.syntax.bifunctor.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*

//import com.github.jasync.sql.db.{Connection, QueryResult}
//import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder
//import com.typesafe.config.{Config, ConfigFactory}
import io.circe.Encoder
import io.circe.syntax.given
import io.circe.generic.auto.*
import io.circe.parser.decode
import org.web3j.abi.{
  EventEncoder,
//  FunctionEncoder,
  FunctionReturnDecoder,
  TypeReference,
}
import org.web3j.abi.datatypes.{Address, Event, Type}
import org.web3j.abi.datatypes.generated.Uint256
//import org.web3j.crypto.{Credentials, MnemonicUtils}
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.{
  DefaultBlockParameter,
//  DefaultBlockParameterName,
}
import org.web3j.protocol.core.methods.request.{EthFilter}
import org.web3j.protocol.core.methods.response.EthLog.{LogResult, LogObject}
//import org.web3j.protocol.core.methods.response.TransactionReceipt
//import org.web3j.protocol.http.HttpService
//import org.web3j.tx.RawTransactionManager
//import org.web3j.tx.response.PollingTransactionReceiptProcessor
import sttp.client3.*
import sttp.model.{MediaType, StatusCode}

import lib.crypto.CryptoOps//, KeyPair}
//import lib.crypto.Hash.ops.*
import lib.crypto.Sign.ops.*
import lib.datatype.*
import api.model.*
//import api.model.TransactionWithResult.ops.*
import api.model.api_model.{AccountInfo, BalanceInfo}
import api.model.token.*
import common.*
import common.client.*

object EthGatewayDepositMain extends IOApp:

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

      val topics = log.getTopics.asScala.toVector
      val from: Address = FunctionReturnDecoder
        .decodeIndexedValue[Address](topics(1), typeRefAddress)
        .asInstanceOf[Address]
      val to: Address = FunctionReturnDecoder
        .decodeIndexedValue[Address](topics(2), typeRefAddress)
        .asInstanceOf[Address]
      val amount = FunctionReturnDecoder
        .decode(log.getData, List(typeRefUint256).asJava)
        .asScala
        .headOption.map: amount =>
          amount
            .asInstanceOf[Uint256]
            .getValue
        .fold(BigInt(0))(BigInt(_))

      TransferTokenEvent(
        blockNumber = BigInt(log.getBlockNumber),
        txHash = log.getTransactionHash,
        from = from.getValue,
        to = to.getValue,
        value = amount,
      )

  def writeUnsentDeposits[F[_]: Async](
      deposits: Seq[TransferTokenEvent],
  ): F[Unit] = Async[F].blocking:
    val path = Paths.get("unsent-deposits.json")
    val json = deposits.asJson.spaces2
    val _ = Files.write(
      path,
      json.getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.TRUNCATE_EXISTING,
    )

  def readUnsentDeposits[F[_]: Async](): F[Seq[TransferTokenEvent]] =
    Async[F].blocking:
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

  def logSentDeposits[F[_]: Async](
      deposits: Seq[(Account, TransferTokenEvent)],
  ): F[Unit] =
    if deposits.isEmpty then Async[F].unit
    else
      Async[F].blocking:
        val path = Paths.get("sent-deposits.logs")
        val jsons =
          deposits.map(_.asJson.noSpaces).mkString("", "\n", "\n")
        val _ = Files.write(
          path,
          jsons.getBytes(StandardCharsets.UTF_8),
          StandardOpenOption.CREATE,
          StandardOpenOption.WRITE,
          StandardOpenOption.APPEND,
        )

  def writeLastBlockRead[F[_]: Async](blockNumber: BigInt): F[Unit] =
    Async[F].blocking:
      val path = Paths.get("last-block-read.json")
      val json = blockNumber.asJson.spaces2
      val _ = Files.write(
        path,
        json.getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING,
      )

  def readLastBlockRead[F[_]: Async](): F[BigInt] = Async[F].blocking:
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

  def getTransferTokenEvents[F[_]: Async](
      web3j: Web3j,
      contractAddress: String,
      fromBlock: DefaultBlockParameter,
      toBlock: DefaultBlockParameter,
  ): F[Seq[TransferTokenEvent]] =
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

    Async[F]
      .fromCompletableFuture:
        Async[F].delay(web3j.ethGetLogs(filter).sendAsync())
      .map: ethLog =>
        ethLog.getLogs.asScala.map(_.toTransferTokenEvent).toSeq

  def submitTx[F[_]: Async: GatewayKmsClient](
      sttp: SttpBackend[F, Any],
      lmEndpoint: String,
      encryptedLmPrivate: String,
      account: Account,
      tx: Transaction,
  ): F[Unit] = GatewayDecryptService
    .getSimplifiedPlainTextResource[F](encryptedLmPrivate)
    .value
    .flatMap:
      case Left(msg) =>
        scribe.error(s"Failed to get LM private: $msg")
        Async[F].sleep(10.seconds) *> submitTx[F](sttp, lmEndpoint, encryptedLmPrivate, account, tx)
      case Right(lmPrivateResource) =>
        lmPrivateResource.use: lmPrivateArray =>
          val keyPair    = CryptoOps.fromPrivate(BigInt(lmPrivateArray))
          val Right(sig) = keyPair.sign(tx): @unchecked
          val signedTxs  = Seq(Signed(AccountSignature(sig, account), tx))

          scribe.info(s"Sending signed transactions: $signedTxs")

          given bodyJsonSerializer[A: Encoder]: BodySerializer[A] =
            (a: A) =>
              val serialized = a.asJson.noSpaces
              StringBody(serialized, "UTF-8", MediaType.ApplicationJson)

          basicRequest
            .response(asStringAlways)
            .post(uri"http://$lmEndpoint/tx")
            .body(signedTxs)
            .send(sttp)
            .map: response =>
              scribe.info(s"Response: $response")

  def mintLM[F[_]: Async: Clock: GatewayKmsClient](
      sttp: SttpBackend[F, Any],
      lmEndpoint: String,
      encryptedLmPrivate: String,
      toAccount: Account,
      amount: BigInt,
      targetGateway: String,
  ): F[Unit] =

    val networkId = NetworkId(BigNat.unsafeFromLong(1000L))
    val lmDef     = TokenDefinitionId(Utf8.unsafeFrom("LM"))
    val account   = Account(Utf8.unsafeFrom(targetGateway))

    Clock[F].realTimeInstant.flatMap: now =>

      val mintFungibleToken = Transaction.TokenTx.MintFungibleToken(
        networkId = networkId,
        createdAt = now,
        definitionId = lmDef,
        outputs = Map(toAccount -> BigNat.unsafeFromBigInt(amount)),
      )

      submitTx[F](sttp, lmEndpoint, encryptedLmPrivate, account, mintFungibleToken)

  def findAccountByEthAddress[F[_]: Async](
      sttp: SttpBackend[F, Any],
      lmEndpoint: String,
      ethAddress: String,
  ): F[Option[Account]] =

    def findAccountByEthAddress0(
        ethAddress: String,
    ): F[Option[Account]] =
      scribe.info(s"requesting eth address $ethAddress 's LM account")
      Async[F]
        .attempt:
          basicRequest
            .response(asStringAlways)
            .get(uri"http://$lmEndpoint/eth/$ethAddress")
            .send(sttp)
            .map: response =>
              scribe.info(s"eth address $ethAddress response: $response")

              if response.code.isSuccess then
                Some(
                  Account(Utf8.unsafeFrom(response.body.drop(1).dropRight(1))),
                )
              else
                scribe.info(s"Account $ethAddress not found: ${response.body}")
                None
        .map(_.toOption.flatten)

    findAccountByEthAddress0(ethAddress).flatMap: accountOption =>
      if accountOption.isEmpty && ethAddress.startsWith("0x") then
        findAccountByEthAddress0(ethAddress.drop(2))
      else Async[F].pure(accountOption)

  def checkDepositAndMint[F[_]: Async: GatewayKmsClient](
      sttp: SttpBackend[F, Any],
      web3j: Web3j,
      lmEndpoint: String,
      encryptedLmPrivate: String,
      ethContract: String,
      multisigContractAddress: String,
      exemptAddressSet: Set[String],
      startBlockNumber: BigInt,
      endBlockNumber: BigInt,
      targetGateway: String,
  ): F[Unit] =
    for
      events <- getTransferTokenEvents[F](
        web3j,
        ethContract.toLowerCase(Locale.ENGLISH),
        DefaultBlockParameter.valueOf(startBlockNumber.bigInteger),
        DefaultBlockParameter.valueOf(endBlockNumber.bigInteger),
      )
      _ <- Async[F].delay(scribe.info(s"events: $events"))
      depositEvents = events.filter: e =>
        e.to.toLowerCase(Locale.ENGLISH) === multisigContractAddress.toLowerCase(Locale.ENGLISH)
          && !exemptAddressSet.contains(e.to.toLowerCase(Locale.ENGLISH))
      _ <- Async[F].delay:
        scribe.info(s"current deposit events: $depositEvents")
      oldEvents <- readUnsentDeposits[F]()
      _ <- Async[F].delay(scribe.info(s"old deposit events: $oldEvents"))
      allEvents = depositEvents ++ oldEvents
      _ <- Async[F].delay(scribe.info(s"all deposit events: $allEvents"))
      eventAndAccountOptions <- allEvents.toList.traverse: event =>
//        scribe.info(s"current event: $event")
//        val amount    = event.value
//        val toAccount = Account(Utf8.unsafeFrom(event.to))
        findAccountByEthAddress(sttp, lmEndpoint, event.from).map:
          (accountOption: Option[Account]) =>
            scribe.info:
              s"eth address ${event.from}'s LM account: $accountOption"
            (event, accountOption)
      (known, unknown) = eventAndAccountOptions.partition(_._2.nonEmpty)
      toMints = known.flatMap:
        case (event, Some(account)) => List((account, event))
        case (event, None) =>
          scribe.error(s"Internal error: Account ${event.from} not found")
          Nil
      _ <- Async[F].delay(scribe.info(s"toMints: $toMints"))
      _ <- toMints.traverse: (account, event) =>
        mintLM[F](sttp, lmEndpoint, encryptedLmPrivate, account, event.value, targetGateway)
      _ <- logSentDeposits[F](toMints.map(_._1).zip(known.map(_._1)))
      unsent = unknown.map(_._1)
      _ <- Async[F].delay(scribe.info(s"unsent: $unsent"))
      _ <- writeUnsentDeposits[F](unsent)
    yield ()

  def checkLoop[F[_]: Async: GatewayKmsClient](
      sttp: SttpBackend[F, Any],
      web3j: Web3j,
      conf: GatewaySimpleConf,
  ): F[Unit] =
    def run: F[Unit] = for
      _ <- Async[F].delay:
        scribe.info(s"Checking for deposit / withdrawal events")
      lastBlockNumber <- readLastBlockRead[F]()
      startBlockNumber = lastBlockNumber + 1
      blockNumber <- Async[F]
        .fromCompletableFuture(Async[F].delay(web3j.ethBlockNumber.sendAsync()))
        .map(_.getBlockNumber)
        .map(BigInt(_))
      _ <- Async[F].delay(scribe.info(s"blockNumber: $blockNumber"))
      endBlockNumber = (startBlockNumber + 10000) min (blockNumber - 6)
      _ <- Async[F].delay:
        scribe.info(s"startBlockNumber: $startBlockNumber")
        scribe.info:
          s"blockNumber: $blockNumber, endBlockNumber: $endBlockNumber"
      _ <- checkDepositAndMint[F](
        sttp = sttp,
        web3j = web3j,
        lmEndpoint = conf.lmEndpoint,
        encryptedLmPrivate = conf.encryptedLmPrivate,
        ethContract = conf.ethLmContractAddress,
        multisigContractAddress = conf.ethMultisigContractAddress,
        exemptAddressSet = conf.depositExempts.map(_.toLowerCase(Locale.ENGLISH)).toSet,
        startBlockNumber = startBlockNumber,
        endBlockNumber = endBlockNumber,
        targetGateway = conf.targetGateway,
      )
      _ <- writeLastBlockRead[F](endBlockNumber)
      _ <- Async[F].delay(scribe.info(s"Deposit check finished."))
    yield ()

    def loop: F[Unit] = for
      _ <- run.orElse(Async[F].unit)
      _ <- Async[F].sleep(10000.millis)
      _ <- loop
    yield ()

    loop

  def getGasPrice[F[_]: Async](web3j: Web3j): F[BigInt] =
    Async[F]
      .fromCompletableFuture:
        Async[F].delay(web3j.ethGasPrice.sendAsync())
      .map: ethGasPrice =>
        BigInt(ethGasPrice.getGasPrice)

  def getBalance[F[_]: Async](
      sttp: SttpBackend[F, Any],
      lmEndpoint: String,
      targetGateway: String,
  ): F[Option[BalanceInfo]] =
    val lmDef = TokenDefinitionId(Utf8.unsafeFrom("LM"))
    basicRequest
      .response(asStringAlways)
      .get(uri"http://$lmEndpoint/balance/$targetGateway?movable=all")
      .send(sttp)
      .map: response =>
        if response.code.isSuccess then
          decode[Map[TokenDefinitionId, BalanceInfo]](response.body) match
            case Right(balanceInfoMap) => balanceInfoMap.get(lmDef)
            case Left(error) =>
              scribe.error(s"Error decoding balance info: $error")
              scribe.error(s"response: ${response.body}")
              None
        else if response.code.code === StatusCode.NotFound.code then
          scribe.info:
            s"balance of account $targetGateway not found: ${response.body}"
          None
        else
          scribe.error(s"Error getting balance: ${response.body}")
          None

  def getAccountInfo[F[_]: Async](
      sttp: SttpBackend[F, Any],
      lmAddress: String,
      account: Account,
  ): F[Option[AccountInfo]] =
    basicRequest
      .response(asStringAlways)
      .get(uri"http://$lmAddress/account/${account.utf8.value}")
      .send(sttp)
      .map: response =>
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

  def run(args: List[String]): IO[ExitCode] =
    val conf = GatewaySimpleConf.loadOrThrow()
    GatewayResource
      .getSimpleResource[IO](conf)
      .use: (kms, web3j, sttp) =>
        given GatewayKmsClient[IO]      = kms
        EitherT.liftF:
          checkLoop[IO](
            sttp = sttp,
            web3j = web3j,
            conf = conf,
          )
      .value
      .map:
        case Left(error) => scribe.error(s"Error: $error")
        case Right(result) => scribe.info(s"Result: $result")
      .as(ExitCode.Success)
