package io.leisuremeta.chain
package gateway.eth

import java.math.{BigInteger, MathContext}
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.time.Instant
import java.util.{ArrayList, Arrays, Collections, Locale}
import java.util.concurrent.CompletableFuture

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import scala.util.Try

import cats.data.{EitherT, OptionT}
import cats.effect.{Async, Clock, ExitCode, IO, IOApp, Resource}
import cats.syntax.apply.*
import cats.syntax.applicativeError.*
import cats.syntax.either.*
import cats.syntax.eq.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*

import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder
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
import org.web3j.crypto.{Credentials, RawTransaction, TransactionEncoder}
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.{
  DefaultBlockParameter,
  DefaultBlockParameterName,
  Request as Web3jRequest,
  Response as Web3jResponse,
}
import org.web3j.protocol.core.methods.response.EthFeeHistory.FeeHistory
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.exceptions.TransactionException
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.response.PollingTransactionReceiptProcessor
import scodec.bits.{ByteVector, hex}
import software.amazon.awssdk.auth.credentials.{
  AwsCredentials,
  StaticCredentialsProvider,
}
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsAsyncClient
import software.amazon.awssdk.services.kms.model.DecryptRequest
import sttp.client3.*
import sttp.model.{MediaType, StatusCode}

import lib.crypto.{CryptoOps, Hash, KeyPair}
import lib.crypto.Hash.ops.*
import lib.crypto.Sign.ops.*
import lib.datatype.*
import api.model.*
import api.model.TransactionWithResult.ops.*
import api.model.api_model.{AccountInfo, BalanceInfo, NftBalanceInfo}
import api.model.token.*

import common.*
import common.client.*
import org.web3j.abi.FunctionReturnDecoder
import java.nio.charset.StandardCharsets

object EthGatewayWithdrawMain extends IOApp:

  def submitTx[F[_]: Async: GatewayKmsClient](
      sttp: SttpBackend[F, Any],
      lmAddress: String,
      account: Account,
      tx: Transaction,
      encryptedLmPrivateBase64: String,
  ): F[Unit] = GatewayDecryptService
    .getSimplifiedPlainTextResource[F](encryptedLmPrivateBase64)
    .value
    .flatMap:
      case Left(msg) =>
        scribe.error(s"Failed to get LM private: $msg")
        Async[F].sleep(10.seconds) *> submitTx[F](
          sttp,
          lmAddress,
          account,
          tx,
          encryptedLmPrivateBase64,
        )
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
            .post(uri"http://$lmAddress/tx")
            .body(signedTxs)
            .send(sttp)
            .map: response =>
              scribe.info(s"Response: $response")

  def initialLmSecretCheck[F[_]: Async: GatewayKmsClient](
      sttp: SttpBackend[F, Any],
      web3j: Web3j,
      conf: GatewaySimpleConf,
  ): EitherT[F, String, Unit] = for
    gatewayInfoResponse <- EitherT.liftF:
      basicRequest
        .response(asStringAlways)
        .get(uri"http://${conf.lmEndpoint}/account/${conf.targetGateway}")
        .send(sttp)
    gatewayAccountInfo <- EitherT.fromEither[F]:
      decode[AccountInfo](gatewayInfoResponse.body).leftMap(_.getMessage())
    lmSecretResource <- GatewayDecryptService
      .getSimplifiedPlainTextResource[F](conf.encryptedLmPrivate)
    pks <- EitherT.liftF:
      lmSecretResource.use: lmSecretArray =>
        Async[F].delay:
          val keyPair = CryptoOps.fromPrivate(BigInt(1, lmSecretArray))
          PublicKeySummary.fromPublicKeyHash(keyPair.publicKey.toHash)
    _ <- EitherT.cond[F](
      gatewayAccountInfo.publicKeySummaries.contains(pks),
      (),
      s"Fail to check lm secret. ${conf.targetGateway} does not have pks ${pks}",
    )
  yield ()

  def checkLoop[F[_]: Async: GatewayKmsClient](
      sttp: SttpBackend[F, Any],
      web3j: Web3j,
      conf: GatewaySimpleConf,
  ): F[Unit] =
    def run: F[Unit] = for
      _ <- Async[F].delay(scribe.info(s"Withdrawal check started"))
      _ <- checkLmWithdrawal[F](
        sttp,
        web3j,
        conf,
      )
      _ <- Async[F].delay(scribe.info(s"Withdrawal check finished"))
    yield ()

    def loop: F[Unit] = for
      _ <- run.orElse(Async[F].unit)
      _ <- Async[F].sleep(10000.millis)
      _ <- loop
    yield ()

    loop

  def getFungibleBalance[F[_]: Async](
      sttp: SttpBackend[F, Any],
      lmEndpoint: String,
      targetGateway: String,
  ): F[Map[TokenDefinitionId, BalanceInfo]] = basicRequest
    .response(asStringAlways)
    .get(uri"http://$lmEndpoint/balance/$targetGateway?movable=free")
    .send(sttp)
    .map: response =>
      if response.code.isSuccess then
        decode[Map[TokenDefinitionId, BalanceInfo]](response.body) match
          case Right(balanceInfoMap) => balanceInfoMap
          case Left(error) =>
            scribe.error(s"Error decoding balance info: $error")
            scribe.error(s"response: ${response.body}")
            Map.empty
      else if response.code.code === StatusCode.NotFound.code then
        scribe.info(
          s"balance of account $targetGateway not found: ${response.body}",
        )
        Map.empty
      else
        scribe.error(s"Error getting balance: ${response.body}")
        Map.empty

  def getNftBalance[F[_]: Async](
      sttp: SttpBackend[F, Any],
      lmEndpoint: String,
      targetGateway: String,
  ): F[Map[TokenId, NftBalanceInfo]] = basicRequest
    .response(asStringAlways)
    .get(uri"http://$lmEndpoint/nft-balance/$targetGateway?movable=free")
    .send(sttp)
    .map: response =>
      if response.code.isSuccess then
        decode[Map[TokenId, NftBalanceInfo]](response.body) match
          case Right(balanceInfoMap) => balanceInfoMap
          case Left(error) =>
            scribe.error(s"Error decoding nft-balance info: $error")
            scribe.error(s"response: ${response.body}")
            Map.empty
      else if response.code.code === StatusCode.NotFound.code then
        scribe.info(
          s"nft-balance of account $targetGateway not found: ${response.body}",
        )
        Map.empty
      else
        scribe.error(s"Error getting nft-balance: ${response.body}")
        Map.empty

  def getAccountInfo[F[_]: Async](
      sttp: SttpBackend[F, Any],
      lmEndpoint: String,
      account: Account,
  ): F[Option[AccountInfo]] = basicRequest
    .response(asStringAlways)
    .get(uri"http://$lmEndpoint/account/${account.utf8.value}")
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

  def checkLmWithdrawal[F[_]: Async: Clock: GatewayKmsClient](
      sttp: SttpBackend[F, Any],
      web3j: Web3j,
      conf: GatewaySimpleConf,
  ): F[Unit] = getFungibleBalance(sttp, conf.lmEndpoint, conf.targetGateway)
    .flatMap { (balanceMap: Map[TokenDefinitionId, BalanceInfo]) =>

      val gatewayAccount = Account(Utf8.unsafeFrom(conf.targetGateway))
      val LM             = TokenDefinitionId(Utf8.unsafeFrom("LM"))

      balanceMap
        .get(LM)
        .toList
        .flatMap(_.unused.toSeq)
        .filterNot(_._2.signedTx.sig.account === gatewayAccount)
        .traverse { case (txHash, txWithResult) =>
          txWithResult.signedTx.value match
            case tx: Transaction.TokenTx.TransferFungibleToken =>
              {
                scribe.info:
                  s"Try to handle ${txWithResult.signedTx.sig.account}'s tx: $tx"
                for
                  amount <- EitherT.fromOption[F](
                    tx.outputs.get(gatewayAccount),
                    s"No output amount to send to gateway",
                  )
                  accountInfo <- EitherT.fromOptionF(
                    getAccountInfo(
                      sttp,
                      conf.lmEndpoint,
                      txWithResult.signedTx.sig.account,
                    ),
                    s"No account info of ${txWithResult.signedTx.sig.account}",
                  )
                  ethAddress <- EitherT.fromOption[F](
                    accountInfo.ethAddress,
                    s"No eth address of ${txWithResult.signedTx.sig.account}",
                  )
                  _ <- EitherT.liftF:
                    requestEthLmMultisigTransfer(
                      web3j = web3j,
                      ethChainId = conf.ethChainId,
                      multiSigContractAddress = conf.ethMultisigContractAddress,
                      encryptedEthPrivate = conf.encryptedEthPrivate,
                      gatewayEthAddress = conf.gatewayEthAddress,
                      txId = txHash,
                      receiverEthAddress = ethAddress.utf8.value,
                      amount = amount,
                    )
                  now <- EitherT.liftF(Clock[F].realTimeInstant)
                  tx1 = Transaction.TokenTx.TransferFungibleToken(
                    networkId = NetworkId(BigNat.unsafeFromLong(1000L)),
                    createdAt = now,
                    tokenDefinitionId = LM,
                    inputs = Set(txHash.toSignedTxHash),
                    outputs = Map(gatewayAccount -> amount),
                    memo = Some(Utf8.unsafeFrom {
                      s"After withdrawing of ${txWithResult.signedTx.sig.account}'s $amount"
                    }),
                  )
                  _ <- EitherT.liftF:
                    submitTx(
                      sttp,
                      conf.lmEndpoint,
                      gatewayAccount,
                      tx1,
                      conf.encryptedLmPrivate,
                    )
                yield ()
              }.leftMap { msg =>
                scribe.error(msg)
                msg
              }.value
            case _ => Async[F].delay(().asRight[String])
        }
    }
    .as(())

//  def checkNftWithdrawal[F[_]
//    : Async: Clock: GatewayApiClient: GatewayDatabaseClient: GatewayKmsClient](
//      sttp: SttpBackend[F, Any],
//      web3j: Web3j,
//      conf: GatewayConf,
//  ): F[Unit] = getNftBalance(sttp, conf.lmEndpoint, conf.targetGateway)
//    .flatMap { (balanceMap: Map[TokenId, NftBalanceInfo]) =>
//
//      val gatewayAccount = Account(Utf8.unsafeFrom(conf.targetGateway))
//
//      balanceMap.toSeq.traverse { case (tokenId, balanceInfo) =>
//        balanceInfo.tx.signedTx.value match
//          case tx: Transaction.TokenTx.TransferNFT
//              if balanceInfo.tx.signedTx.sig.account =!= gatewayAccount =>
//            {
//              for
//                info <- OptionT:
//                  getAccountInfo(
//                    sttp,
//                    conf.lmEndpoint,
//                    balanceInfo.tx.signedTx.sig.account,
//                  )
//                ethAddress <- OptionT.fromOption[F](info.ethAddress)
//                _ <- OptionT.liftF:
//                  mintEthNft[F](
//                    web3j = web3j,
//                    ethChainId = conf.ethChainId,
//                    ethNftContract = conf.ethContractAddress,
//                    gatewayEthAddress = conf.gatewayEthAddress,
//                    receiverEthAddress = ethAddress.utf8.value,
//                    tokenId = tokenId,
//                  )
//                now <- OptionT.liftF(Clock[F].realTimeInstant)
//                tx1 = tx.copy(
//                  createdAt = now,
//                  input = balanceInfo.tx.signedTx.toHash,
//                  output = gatewayAccount,
//                  memo =
//                    Some(Utf8.unsafeFrom("gateway balance after withdrawal")),
//                )
//                _ <- OptionT.liftF:
//                  submitTx[F](sttp, conf.lmEndpoint, gatewayAccount, tx1)
//              yield ()
//            }.value
//          case _ => Async[F].delay(None)
//      }
//    }
//    .as(())

//  def transferEthLM[F[_]
//    : Async: GatewayApiClient: GatewayDatabaseClient: GatewayKmsClient](
//      web3j: Web3j,
//      ethChainId: Int,
//      ethLmContract: String,
//      gatewayEthAddress: String,
//      receiverEthAddress: String,
//      amount: BigNat,
//  ): F[Unit] =
//
//    scribe.info(s"Transfer eth LM to ${receiverEthAddress}")
//
//    val mintParams = new ArrayList[Type[?]]()
//    mintParams.add(new Address(receiverEthAddress))
//    mintParams.add(new Uint256(amount.toBigInt.bigInteger))
//
//    val returnTypes = Collections.emptyList[TypeReference[?]]()
//
//    val transferTxData = FunctionEncoder.encode:
//      new Function("transfer", mintParams, returnTypes)
//
//    sendEthTransaction[F](
//      web3j = web3j,
//      ethChainId = ethChainId,
//      contractAddress = ethLmContract,
//      txData = transferTxData,
//      gatewayEthAddress = gatewayEthAddress,
//    ).as(())

  def requestEthLmMultisigTransfer[F[_]: Async: GatewayKmsClient](
      web3j: Web3j,
      ethChainId: Int,
      gatewayEthAddress: String,
      multiSigContractAddress: String,
      encryptedEthPrivate: String,
      txId: Hash.Value[TransactionWithResult],
      receiverEthAddress: String,
      amount: BigNat,
  ): F[Unit] =

    scribe.info(s"Transfer eth LM to ${receiverEthAddress}")

    val mintParams = new ArrayList[Type[?]]()
    mintParams.add(new Uint256(txId.toUInt256Bytes.toBigInt.bigInteger))
    mintParams.add(new Address(receiverEthAddress))
    mintParams.add(new Uint256(amount.toBigInt.bigInteger))

    val returnTypes = Collections.emptyList[TypeReference[?]]()

    val transferTxData = FunctionEncoder.encode:
      new Function("addTransaction", mintParams, returnTypes)

    sendEthTransaction[F](
      web3j = web3j,
      ethChainId = ethChainId,
      contractAddress = multiSigContractAddress,
      txData = transferTxData,
      gatewayEthAddress = gatewayEthAddress,
      encryptedEthPrivate = encryptedEthPrivate,
    ).as(())

//  def mintEthNft[F[_]
//    : Async: GatewayApiClient: GatewayDatabaseClient: GatewayKmsClient](
//      web3j: Web3j,
//      ethChainId: Int,
//      ethNftContract: String,
//      gatewayEthAddress: String,
//      receiverEthAddress: String,
//      tokenId: TokenId,
//  ): F[Unit] =
//
//    val tokenIdBigInt = BigInt(tokenId.utf8.value)
//
//    val mintParams = new ArrayList[Type[?]]()
//    mintParams.add(new Address(receiverEthAddress))
//    mintParams.add(new Uint256(tokenIdBigInt.bigInteger))
//
//    val returnTypes = Collections.emptyList[TypeReference[?]]()
//
//    val mintTxData = FunctionEncoder.encode {
//      new Function("safeMint", mintParams, returnTypes)
//    }
//
//    sendEthTransaction[F](
//      web3j = web3j,
//      ethChainId = ethChainId,
//      contractAddress = ethNftContract,
//      txData = mintTxData,
//      gatewayEthAddress = gatewayEthAddress,
//    ).as(())

  def requestToF[F[_]: Async, A, B, C <: Web3jResponse[B], D](
      request: Web3jRequest[A, C],
  )(map: C => D): F[D] =
    Async[F]
      .recoverWith:
        Async[F]
          .fromCompletableFuture(Async[F].delay(request.sendAsync()))
          .map(map)
      .apply:
        case t: Throwable =>
          scribe.error(t)
          Async[F].sleep(10.seconds) *> requestToF(request)(map)

  def sendEthTransaction[F[_]: Async: GatewayKmsClient](
      web3j: Web3j,
      ethChainId: Int,
      contractAddress: String,
      txData: String,
      gatewayEthAddress: String,
      encryptedEthPrivate: String,
  ): F[Unit] = GatewayDecryptService
    .getSimplifiedPlainTextResource[F](encryptedEthPrivate)
    .value
    .flatMap:
      case Left(msg) =>
        scribe.error(s"Fail to get eth private key: $msg")
        Async[F].sleep(10.seconds) *> sendEthTransaction[F](
          web3j,
          ethChainId,
          contractAddress,
          txData,
          gatewayEthAddress,
          encryptedEthPrivate,
        )
      case Right(ethResource) =>
        ethResource.use: ethPrivateByteArray =>
          val ethPrivate = ByteVector.view(ethPrivateByteArray).toHex
          val credential = Credentials.create(ethPrivate)
          assert(
            credential.getAddress() === gatewayEthAddress
              .toLowerCase(Locale.ENGLISH),
            s"invalid gateway eth address: ${credential.getAddress} vs $gatewayEthAddress",
          )
          val TX_END_CHECK_DURATION = 20000
          val TX_END_CHECK_RETRY    = 9
          val receiptProcessor = new PollingTransactionReceiptProcessor(
            web3j,
            TX_END_CHECK_DURATION,
            TX_END_CHECK_RETRY,
          )
          val manager =
            new RawTransactionManager(
              web3j,
              credential,
              ethChainId,
              receiptProcessor,
            )

          val GAS_LIMIT = 600_000

          def loop(
              lastTrial: Option[(BigInteger, BigInteger, String)],
          ): F[Unit] =

            def getMaxPriorityFeePerGas(): F[BigInteger] =
              requestToF(web3j.ethMaxPriorityFeePerGas()):
                _.getMaxPriorityFeePerGas()

            def getBaseFee(): F[BigInteger] =
              val blockCount: String = BigInt(9).toString(16)
              val newestBlock: DefaultBlockParameter =
                DefaultBlockParameterName.LATEST
              val rewardPercentiles: java.util.List[java.lang.Double] =
                ArrayBuffer[java.lang.Double](0, 0.5, 1, 1.5, 3, 80).asJava

              requestToF {
                web3j.ethFeeHistory(blockCount, newestBlock, rewardPercentiles)
              }.apply: response =>
                val history = response.getFeeHistory()

                val baseFees = history.getBaseFeePerGas().asScala.toList

                val mean = BigDecimal(baseFees.map(BigInt(_)).sum) / 10
                val std = baseFees
                  .map(x =>
                    BigDecimal(
                      (BigDecimal(x) - mean)
                        .pow(2)
                        .bigDecimal
                        .sqrt(MathContext.DECIMAL32),
                    ),
                  )
                  .sum / 10
                val targetBaseFees = (mean + std + 0.5).toBigInt

                targetBaseFees.bigInteger

            def getNonce(): F[BigInteger] = requestToF {
              web3j.ethGetTransactionCount(
                gatewayEthAddress,
                DefaultBlockParameterName.LATEST,
              )
            }(_.getTransactionCount())

            def sendNewTx(
                baseFee: BigInteger,
                maxPriorityFeePerGas: BigInteger,
            ): F[Option[String]] = for
              nonce <- getNonce()
              _     <- Async[F].delay { scribe.info(s"Nonce: $nonce") }
              tx = RawTransaction.createTransaction(
                ethChainId,
                nonce,
                BigInteger.valueOf(GAS_LIMIT),
                contractAddress,
                BigInteger.ZERO,
                txData,
                maxPriorityFeePerGas,
                baseFee `add` maxPriorityFeePerGas,
              )
              txResponseOption <- Async[F]
                .blocking { manager.signAndSend(tx) }
                .map: resp =>
                  if resp.hasError() then
                    val e = resp.getError()
                    scribe.info:
                      s"Error in sending tx: #(${e.getCode()}) ${e.getMessage()}"
                    writeFailLog(txData)
                  else scribe.info(s"Sending Eth Tx: ${resp.getResult()}")
                  Option(resp.getResult)
            yield txResponseOption

            def getReceipt(
                txResponse: String,
            ): F[Either[Throwable, TransactionReceipt]] =
              Async[F].blocking:
                Try(
                  receiptProcessor.waitForTransactionReceipt(txResponse),
                ).toEither

            for
              _ <- Async[F].delay { scribe.info(s"Last trial: ${lastTrial}") }
              baseFee <- getBaseFee()
              _ <- Async[F].delay { scribe.info(s"New base fee: ${baseFee}") }
              maxPriorityFeePerGas <- getMaxPriorityFeePerGas()
              _ <- Async[F].delay:
                scribe.info(s"Max Priority Fee Per Gas: $maxPriorityFeePerGas")
              txIdOption <- lastTrial match
                case Some((oldBaseFee, oldPriorityFee, txId))
                    if (oldBaseFee `add` oldPriorityFee).compareTo(
                      baseFee `add` maxPriorityFeePerGas,
                    ) >= 0 =>
                  scribe.info(s"New base fee is less than old one")
                  Async[F].pure(Some(txId))
                case _ =>
                  sendNewTx(baseFee, maxPriorityFeePerGas).map:
                    _.orElse(lastTrial.map(_._3))
              _ <- txIdOption match
                case Some(txId) =>
                  for
                    receiptEither <- getReceipt(txId)
                    _ <- receiptEither match
                      case Left(e) =>
                        e match
                          case te: TransactionException =>
                            scribe.info(s"Timeout: ${te.getMessage()}")
                            Async[F].delay(())
//                            loop(Some(baseFee, maxPriorityFeePerGas, txId))
                          case _ =>
                            scribe.error:
                              s"Fail to send transaction: ${e.getMessage()}"
                            Async[F].delay(())
//                            loop(Some(baseFee, maxPriorityFeePerGas, txId))
                      case Right(receipt) =>
                        if receipt.isStatusOK() then
                          Async[F].delay:
                            scribe.info:
                              s"transaction ${receipt.getTransactionHash()} saved to block #${receipt.getBlockNumber()}"
                            // BigInt(receipt.getBlockNumber())
                            ()
                        else
                          scribe.error:
                            s"transaction ${receipt.getTransactionHash()} failed with receipt:${receipt}"
                          Async[F].delay(())
//                          loop(None)
                  yield ()
                case None =>
//                  Async[F].sleep(1.minute) *> loop(None)
                  Async[F].delay(())
            yield ()

          loop(None)

  @SuppressWarnings(Array("org.wartremover.warts.MutableDataStructures"))
  def writeFailLog[F[_]: Async](txData: String): F[Unit] = Async[F].blocking:
    val typeRefAddress: TypeReference[Type[?]] = (new TypeReference[Address]() {}).asInstanceOf[TypeReference[Type[?]]]
    val typeRefUint256: TypeReference[Type[?]] = (new TypeReference[Uint256]() {}).asInstanceOf[TypeReference[Type[?]]]
    val parseTx = FunctionReturnDecoder.decode(txData.drop(10), List(typeRefUint256, typeRefAddress, typeRefUint256).asJava).asScala
    val path = Paths.get("failed_tx")
    val res = parseTx.map(e => e.getValue).mkString("", " ", "\n")
    Files.write(
      path,
      res.getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.APPEND,
    )

  def run(args: List[String]): IO[ExitCode] =
    val conf = GatewaySimpleConf.loadOrThrow()
    GatewayResource
      .getSimpleResource[IO](conf)
      .use: (kms, web3j, sttp) =>
        given GatewayKmsClient[IO] = kms
        initialLmSecretCheck[IO](sttp, web3j, conf) *> EitherT.liftF:
          checkLoop[IO](
            sttp = sttp,
            web3j = web3j,
            conf = conf,
          )
      .value
      .map:
        case Left(error)   => scribe.error(s"Error: $error")
        case Right(result) => scribe.info(s"Result: $result")
      .as(ExitCode.Success)
