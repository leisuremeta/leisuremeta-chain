package io.leisuremeta.chain.gateway.eth.setup

import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*

import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import cats.syntax.flatMap.*
import cats.syntax.functor.*

import com.github.jasync.sql.db.{Connection, QueryResult}
import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder

import scodec.bits.ByteVector

import software.amazon.awssdk.auth.credentials.{
  AwsCredentials,
  StaticCredentialsProvider,
}
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsAsyncClient
import software.amazon.awssdk.services.kms.model.{
  DecryptRequest,
  GenerateDataKeyRequest,
}
import software.amazon.awssdk.services.kms.model.DataKeySpec
import software.amazon.awssdk.services.kms.model.EncryptRequest
import software.amazon.awssdk.services.kms.model.EncryptionAlgorithmSpec

object EthGatewaySetupMain extends IOApp:

  def connectDatabase[F[_]: Async](
      host: String,
      db: String,
      table: String,
      user: String,
      password: String,
  ): Resource[F, Connection] =
    Resource
      .make:
        Async[F].blocking:
          MySQLConnectionBuilder.createConnectionPool:
            s"jdbc:mysql://${host}:3306/${db}?user=${user}&password=${password}"
      .apply: connection =>
        Async[F]
          .fromCompletableFuture:
            Async[F].delay(connection.disconnect())
          .map(_ => ())

  def connectKms[F[_]: Async]: Resource[F, KmsAsyncClient] =
    Resource.fromAutoCloseable(Async[F].delay(KmsAsyncClient.create()))

  def encrypt[F[_]: Async](kmsClient: KmsAsyncClient, alias: String)(
      data: Array[Byte],
  ): F[Array[Byte]] =
    Async[F]
      .fromCompletableFuture:
        Async[F].delay:
          kmsClient.encrypt:
            EncryptRequest
              .builder()
              .keyId(s"alias/${alias}")
              .plaintext(SdkBytes.fromByteArray(data))
              .build()
      .map(_.ciphertextBlob().asByteArrayUnsafe())

  def decrypt[F[_]: Async](kmsClient: KmsAsyncClient, alias: String)(
      data: Array[Byte],
  ): F[Array[Byte]] =
    Async[F]
      .fromCompletableFuture:
        Async[F].delay:
          kmsClient.decrypt:
            DecryptRequest
              .builder()
              .keyId(s"alias/${alias}")
              .ciphertextBlob(SdkBytes.fromByteArray(data))
              .build()
      .map(_.plaintext().asByteArrayUnsafe())

  def encryptDepositSide[F[_]: Async](
      kmsClient: KmsAsyncClient,
      config: EthGatewaySetupConfig,
  )(data: Array[Byte]): F[Array[Byte]] =
    for
      cipherText1 <- encrypt[F](kmsClient, config.depositKmsAlias)(data)
      cipherText2 <- encrypt[F](kmsClient, config.withdrawKmsAlias)(cipherText1)
    yield cipherText2

  def encryptWithdrawSide[F[_]: Async](
      kmsClient: KmsAsyncClient,
      config: EthGatewaySetupConfig,
  )(data: Array[Byte]): F[Array[Byte]] =
    for
      cipherText1 <- encrypt[F](kmsClient, config.withdrawKmsAlias)(data)
      cipherText2 <- encrypt[F](kmsClient, config.depositKmsAlias)(cipherText1)
    yield cipherText2

  def divide(cipherText: Array[Byte]): (String, String) =
    val bytes         = ByteVector(cipherText)
    val (front, back) = bytes.splitAt(bytes.size / 2)
    (front.toBase64, back.toBase64)

  def saveToDatabase[F[_]: Async](conn: Connection)(
      tableName: String,
      valueColumn: String,
      key: String,
      value: String,
  ): F[Unit] = Async[F]
    .fromCompletableFuture:
      Async[F].delay:
        conn.sendPreparedStatement(
          s"INSERT INTO ${tableName} (GTWY_SE_CODE, ${valueColumn}) VALUES (?, ?) ON DUPLICATE KEY UPDATE GTWY_SE_CODE = ?",
          List(key, value, value).asJava,
        )
    .map: (queryResult: QueryResult) =>
      scribe.info(s"Query result: ${queryResult}")
      ()

  def encryptAndDivide[F[_]: Async](encrypt: Array[Byte] => F[Array[Byte]])(
      plainText: Array[Byte],
  ): F[(String, String)] =
    for
      cipherText <- encrypt(plainText)
      (front, back) = divide(cipherText)
    yield (front, back)

  def saveFrontAndBack[F[_]: Async](
    frontDb: Connection,
    frontTable: String,
    frontValueColumn: String,
    backDb: Connection,
    backTable: String,
    backValueColumn: String,
    frontAndBack: Map[String, (String, String)],
    key: String,
  ): F[Unit] =
    for
      _ <- saveToDatabase[F](frontDb)(frontTable, key, frontValueColumn, frontAndBack(key)._1)
      _ <- saveToDatabase[F](backDb)(backTable, key, backValueColumn, frontAndBack(key)._2)
    yield ()

  def encryptAndSave[F[_]: Async](encrypt: Array[Byte] => F[Array[Byte]])(
    frontDb: Connection,
    frontTable: String,
    frontValueColumn: String,
    backDb: Connection,
    backTable: String,
    backValueColumn: String,
    key: String,
    plainText: Array[Byte],
  ): F[Unit] =
    for
      cipherText <- encrypt(plainText)
      (front, back) = divide(cipherText)
      _ <- saveToDatabase[F](frontDb)(frontTable, key, frontValueColumn, front)
      _ <- saveToDatabase[F](backDb)(backTable, key, backValueColumn, back)
    yield ()

  def hexToByteArray(hex: String): Array[Byte] =
    ByteVector.fromValidHex(hex).toArrayUnsafe

  def allEncryptAndDivide[F[_]: Async](
    config: EthGatewaySetupConfig,
    kmsClient: KmsAsyncClient,
  ): F[Map[String, (String, String)]] = for
    lmd <- encryptAndDivide[F](encryptDepositSide[F](kmsClient, config))(
      hexToByteArray(config.lmPrivate),
    )
    lm <- encryptAndDivide[F](encryptWithdrawSide[F](kmsClient, config))(
      hexToByteArray(config.lmPrivate),
    )
    eth <- encryptAndDivide[F](encryptWithdrawSide[F](kmsClient, config))(
      hexToByteArray(config.ethPrivate),
    )
  yield Map("LM-D" -> lmd, "LM" -> lm, "ETH" -> eth)

  def allSaveFrontAndBack[F[_]: Async](
    config: EthGatewaySetupConfig,
    depositDb: Connection,
    withdrawDb: Connection,
    frontAndBack: Map[String, (String, String)],
  ): F[Unit] = for
    _ <- saveFrontAndBack[F](
      depositDb,
      config.depositDb.table,
      config.depositDb.valueColumn,
      withdrawDb,
      config.withdrawDb.table,
      config.withdrawDb.valueColumn,
      frontAndBack,
      "LM-D",
    )
    _ <- saveFrontAndBack[F](
      withdrawDb,
      config.withdrawDb.table,
      config.withdrawDb.valueColumn,
      depositDb,
      config.depositDb.table,
      config.depositDb.valueColumn,
      frontAndBack,
      "LM",
    )
    _ <- saveFrontAndBack[F](
      withdrawDb,
      config.withdrawDb.table,
      config.withdrawDb.valueColumn,
      depositDb,
      config.depositDb.table,
      config.depositDb.valueColumn,
      frontAndBack,
      "ETH",
    )
  yield ()

  def run(args: List[String]): IO[ExitCode] =

    val config = EthGatewaySetupConfig()

    val resources = for
      kmsClient <- connectKms[IO]
      depositDb <- connectDatabase[IO](
        config.depositDb.host,
        config.depositDb.db,
        config.depositDb.table,
        config.dbWriteAccount.user,
        config.dbWriteAccount.password,
      )
      withdrawDb <- connectDatabase[IO](
        config.withdrawDb.host,
        config.withdrawDb.db,
        config.withdrawDb.table,
        config.dbWriteAccount.user,
        config.dbWriteAccount.password,
      )
    yield (kmsClient, depositDb, withdrawDb)

    connectKms[IO].use: kmsClient =>
      def dbEndpoint(conf: EthGatewaySetupConfig.DbConfig): Array[Byte] =
        s"jdbc:mysql://${conf.host}:${conf.port}/${conf.db}?user=${conf.user}&password=${conf.password}".getBytes("UTF-8")

      val depositEndpoint = dbEndpoint(config.depositDb)
      val withdrawEndpoint = dbEndpoint(config.withdrawDb)

      def toBase64(bytes: Array[Byte]): String =
        ByteVector.view(bytes).toBase64

      for
        encryptedDepositDb <- encrypt[IO](kmsClient, config.depositKmsAlias)(depositEndpoint)
        decryptedDepositDb <- decrypt[IO](kmsClient, config.depositKmsAlias)(encryptedDepositDb)
        encryptedWithdrawDb <- encrypt[IO](kmsClient, config.withdrawKmsAlias)(withdrawEndpoint)
        decryptedWithdrawDb <- decrypt[IO](kmsClient, config.withdrawKmsAlias)(encryptedWithdrawDb)
        encryptedEthEndpointWithDepositKey <- encrypt[IO](kmsClient, config.depositKmsAlias):
          config.ethEndpoint.getBytes("UTF-8")
        encryptedEthEndpointWithWithdrawKey <- encrypt[IO](kmsClient, config.withdrawKmsAlias):
          config.ethEndpoint.getBytes("UTF-8")
        decryptedEthEndpointWithDepositKey <- decrypt[IO](kmsClient, config.depositKmsAlias):
          encryptedEthEndpointWithDepositKey
        decryptedEthEndpointWithWithdrawKey <- decrypt[IO](kmsClient, config.withdrawKmsAlias):
          encryptedEthEndpointWithWithdrawKey
      yield
        println(s"Deposit DB: ${toBase64(encryptedDepositDb)}")
        println(s"Decrypted Deposit DB: ${String(decryptedDepositDb, "UTF-8")}")
        println(s"Withdraw DB: ${toBase64(encryptedWithdrawDb)}")
        println(s"Decrypted Withdraw DB: ${String(decryptedWithdrawDb, "UTF-8")}")
        println(s"ETH Endpoint with Deposit Key: ${toBase64(encryptedEthEndpointWithDepositKey)}")
        println(s"ETH Endpoint with Withdraw Key: ${toBase64(encryptedEthEndpointWithWithdrawKey)}")
        println(s"Decrypted ETH Endpoint with Deposit Key: ${String(decryptedEthEndpointWithDepositKey, "UTF-8")}")
        println(s"Decrypted ETH Endpoint with Withdraw Key: ${String(decryptedEthEndpointWithWithdrawKey, "UTF-8")}")
        ExitCode.Success

//    resources.use: (kmsClient, depositDb, withdrawDb) =>
//      allEncryptAndDivide[IO](config, kmsClient)
//        .map: (keys: Map[String, (String, String)]) =>
//          keys.foreach:
//            case (key, (front, back)) =>
//              println(s""""${key}" -> ("${front}", "${back}")""")
//              ()
//        .as(ExitCode.Success)

//      val keys = Map(
//        "LM-D" -> ("", ""),
//        "LM" -> ("", ""),
//        "ETH" -> ("", ""),
//      )
//
//      allSaveFrontAndBack[IO](config, depositDb, withdrawDb, keys)
//        .as(ExitCode.Success)
