package io.leisuremeta.chain.gateway.eth.common.client

import scala.jdk.CollectionConverters.*

import cats.data.EitherT
import cats.effect.{Async, Resource}
import cats.syntax.functor.*

import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder

trait GatewayDatabaseClient[F[_]]:
  def select(
      key: String,
  ): EitherT[F, String, Option[String]]

object GatewayDatabaseClient:

  def apply[F[_]: GatewayDatabaseClient]: GatewayDatabaseClient[F] = summon

  def make[F[_]: Async](
      host: String,
      db: String,
      table: String,
      user: String,
      password: String,
      tableName: String,
      valueColumn: String,
  ): Resource[F, GatewayDatabaseClient[F]] =
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
