package io.leisuremeta.chain
package gateway.eth

import java.math.BigInteger
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.util.{Arrays, ArrayList, Collections}
import java.time.Instant
import scala.jdk.OptionConverters.*
import scala.concurrent.duration.*
import scala.util.Try

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.applicativeError.*
import cats.syntax.eq.*
import cats.syntax.traverse.*

import com.typesafe.config.{Config, ConfigFactory}
import io.circe.Encoder
import io.circe.syntax.given
import io.circe.generic.auto.*
import io.circe.parser.decode
import org.web3j.abi.{FunctionEncoder, TypeReference}
import org.web3j.abi.datatypes.{Address, Function, Type}
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.{Credentials, RawTransaction, TransactionEncoder}
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import sttp.client3.*
import sttp.model.{MediaType, StatusCode}

import lib.crypto.{CryptoOps, KeyPair}
import lib.crypto.Hash.ops.*
import lib.crypto.Sign.ops.*
import lib.datatype.*
import api.model.*
import api.model.api_model.{AccountInfo, BalanceInfo, NftBalanceInfo}
import api.model.token.*
import api.model.TransactionWithResult.ops.*
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.OkHttpClient

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
      //     .addInterceptor(interceptor)
      .build()

    IO(Web3j.build(new HttpService(url, client)))
  }(web3j => IO(web3j.shutdown()))

  def logSentWithdrawal(
      withdrawals: Seq[(Account, BigNat, Signed.TxHash)],
  ): IO[Unit] = IO.blocking {
    val path = Paths.get("sent-withdrawals.logs")
    val jsons =
      withdrawals.toSeq.map(_.asJson.noSpaces).mkString("", "\n", "\n")
    Files.write(
      path,
      jsons.getBytes,
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.APPEND,
    )
  }

  def getLatestTransactionCount(web3j: Web3j, address: String): IO[BigInt] =
    IO.fromCompletableFuture(IO {
      web3j
        .ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)
        .sendAsync
    }).map(_.getTransactionCount)
      .map(BigInt(_))

  def getTransactionReceipt(
      web3j: Web3j,
      txHash: String,
  ): IO[Option[TransactionReceipt]] =
    IO.fromCompletableFuture(IO {
      web3j
        .ethGetTransactionReceipt(txHash)
        .sendAsync
    }).map(_.getTransactionReceipt)
      .map(_.toScala)

  def transferToken(
      web3j: Web3j,
      ethChainId: Int,
      privateKey: String,
      contractAddress: String,
      toAddress: String,
      amount: BigInt,
  ): IO[TransactionReceipt] =

    scribe.info(
      s"transferToken: $contractAddress, $toAddress, $amount",
    )

    val TX_END_CHECK_DURATION = 20000
    val TX_END_CHECK_RETRY    = 9

    val credential = Credentials.create(privateKey)

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

    scribe.debug(s"txData: $txData")

    def ethSendTransaction(lastTxCount: BigInt, gasPrice: BigInt): IO[String] =
      val tx = RawTransaction.createTransaction(
        (lastTxCount + 1).bigInteger,
        gasPrice.bigInteger,
        BigInt(1000000).bigInteger,
        credential.getAddress,
        txData,
      )
      val signedTx = TransactionEncoder.signMessage(tx, ethChainId, credential)
      val txHexString = Numeric.toHexString(signedTx)

      IO.fromCompletableFuture(IO {
        web3j.ethSendRawTransaction(txHexString).sendAsync
      }).map(_.getTransactionHash)

    def waitAndGetTransactionReceipt(
        txHash: String,
        count: Int,
    ): IO[Option[TransactionReceipt]] =
      for
        _ <- IO.sleep(TX_END_CHECK_DURATION.millis)
        ethReceiptOption <- IO.fromCompletableFuture(IO {
          web3j.ethGetTransactionReceipt(txHash).sendAsync
        })
        receiptOption = ethReceiptOption.getTransactionReceipt.toScala
        receiptOption1 <-
          if receiptOption.isDefined then IO.pure(receiptOption)
          else if count > TX_END_CHECK_RETRY then IO.pure(None)
          else waitAndGetTransactionReceipt(txHash, count + 1)
      yield receiptOption1

    case class EthTransactionContext(gasPrice: BigInt, txHash: String)

    def loop(
        originalContext: Option[EthTransactionContext],
    ): IO[TransactionReceipt] = for
      lastTxCount     <- getLatestTransactionCount(web3j, contractAddress)
      currentGasPrice <- getGasPrice(web3j)
      contexEither <- originalContext match
        case None =>
          for
            txHash           <- ethSendTransaction(lastTxCount, currentGasPrice)
            ethReceiptOption <- waitAndGetTransactionReceipt(txHash, 0)
          yield ethReceiptOption match
            case None => Left(EthTransactionContext(currentGasPrice, txHash))
            case Some(receipt) => Right(receipt)
        case Some(EthTransactionContext(gasPrice, txHash)) => ???
      receipt <- contexEither match
        case Left(context)  => loop(Some(context))
        case Right(receipt) => IO.pure(receipt)
    yield receipt

    loop(None)

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

  def checkLoop(
      web3j: Web3j,
      ethChainId: Int,
      lmAddress: String,
      ethContract: String,
      gatewayEthAddress: String,
      keyPair: KeyPair,
  ): IO[Unit] =
    def run: IO[Unit] = for
      _ <- IO.delay(scribe.info(s"Withdrawal check started"))
      _ <- checkWithdrawal(
        web3j,
        ethChainId,
        lmAddress,
        ethContract,
        gatewayEthAddress,
        keyPair,
      )
      _ <- IO.delay(scribe.info(s"Withdrawal check finished"))
      _ <- IO.sleep(10000.millis)
    yield ()

    def loop: IO[Unit] = for
      _ <- run.orElse(IO.unit)
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

      val withdrawCandidates = {
        for
          unusedTxHashAndTx <- balanceInfo.unused.toList
          (txHash, tx) = unusedTxHashAndTx
          createdAt    = tx.signedTx.value.createdAt
          inputAccount = tx.signedTx.sig.account
          item <- tx.signedTx.value match
            case tf: Transaction.TokenTx.TransferFungibleToken
                if inputAccount != gatewayAccount && tf.tokenDefinitionId === lmDef =>
              tf.outputs
                .get(gatewayAccount)
                .map(amount => (inputAccount, amount))
                .toSeq
            case _ => Seq.empty
        yield (createdAt, txHash, item)
      }.sortBy(_._1)

      scribe.info(s"withdrawCandidates: $withdrawCandidates")

      if withdrawCandidates.isEmpty then
        IO.delay(scribe.info("No withdraw candidates"))
      else
        for
          toWithdrawOptionSeq <- withdrawCandidates.traverse {
            case (createdAt, txHash, (account, amount)) =>
              getAccountInfo(lmAddress, account).map { accountInfoOption =>
                for
                  accountInfo <- accountInfoOption
                  ethAddress  <- accountInfo.ethAddress
                yield (createdAt, txHash, account, ethAddress, amount)
              }
          }
          toWithdraw           = toWithdrawOptionSeq.flatten
          toWithdrawHeadOption = toWithdraw.headOption
          _ <- IO.delay(scribe.info(s"toWithdraw: $toWithdraw"))
          _ <- IO.delay(
            scribe.info(s"toWithdrawHeadOption: $toWithdrawHeadOption"),
          )
          _ <- toWithdrawHeadOption match
            case None => IO.unit
            case Some((createdAt, txHash, account, ethAddress, amount)) =>
              for
                successfulWithdraw <- transferToken(
                  web3j = web3j,
                  ethChainId = ethChainId,
                  privateKey = keyPair.privateKey.toString(16),
                  contractAddress = ethContract,
                  toAddress = ethAddress.utf8.value,
                  amount = amount.toBigInt,
                )
                _ <- IO.delay(scribe.info(s"withdrawal txs are sent"))
                tx = Transaction.TokenTx.TransferFungibleToken(
                  networkId = networkId,
                  createdAt = Instant.now,
                  tokenDefinitionId = lmDef,
                  inputs = Set(txHash.toSignedTxHash),
                  outputs = Map(
                    gatewayAccount -> amount,
                  ),
                  memo = None,
                )
                _ <- IO.delay(submitTx(lmAddress, gatewayAccount, keyPair, tx))
                _ <- IO.delay(scribe.info(s"withdrawal utxos are removed"))
              yield ()
        yield ()
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
          keyPair = keyPair,
        )
      }
    yield ExitCode.Success
