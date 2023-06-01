package io.leisuremeta.chain.gateway.eth.common.client

import scala.jdk.CollectionConverters.*

import cats.data.EitherT
import cats.effect.{Async, Resource}
import cats.syntax.functor.*

import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder
import com.github.jasync.sql.db.ConnectionPoolConfigurationBuilder

trait GatewayDatabaseClient[F[_]]:
  def select(
      key: String,
  ): EitherT[F, String, Option[String]]

object GatewayDatabaseClient:

  def apply[F[_]: GatewayDatabaseClient]: GatewayDatabaseClient[F] = summon

  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  def make[F[_]: Async](
      dbEndpoint: String,
      tableName: String,
      valueColumn: String,
  ): Resource[F, GatewayDatabaseClient[F]] =
    Resource
      .make:
        Async[F].blocking:
          val reg = """^jdbc:mysql://(.*):(\d{4,5})/(.*)\?user=(.*)&password=(.*)$""".r
          val builder = for 
            res <- reg.findFirstMatchIn(dbEndpoint)
            config = ConnectionPoolConfigurationBuilder()
            _ = config.setHost(res.group(1))
            _ = config.setPort(res.group(2).toInt)
            _ = config.setDatabase(res.group(3))
            _ = config.setUsername(res.group(4))
            _ = config.setPassword(res.group(5))
          yield config
          MySQLConnectionBuilder.createConnectionPool(builder.get)
      .apply: connection =>
        Async[F]
          .fromCompletableFuture:
            Async[F].delay(connection.disconnect())
          .map(_ => ())
      .map: connection =>
        new GatewayDatabaseClient[F]:
          override def select(key: String): EitherT[F, String, Option[String]] =
            Async[F]
              .attemptT:
                Async[F]
                  .fromCompletableFuture:
                    Async[F].delay:
                      connection.sendPreparedStatement(
                        s"SELECT GTWY_SE_CODE, ${valueColumn} FROM ${tableName} WHERE GTWY_SE_CODE = ?",
                        List(key).asJava,
                      )
                  .map: (queryResult: QueryResult) =>
                    scribe.info(s"Query result: ${queryResult}")
                    queryResult.getRows.asScala.headOption.map(
                      _.getString(valueColumn),
                    )
              .leftMap(_.getMessage())
