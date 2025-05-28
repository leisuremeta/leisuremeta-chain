package io.leisuremeta.chain
package lmscan.agent

import cats.effect.*
import cats.implicits.*
import cats.data.*
import cats.effect.kernel.instances.all.*
import sttp.client3.SttpBackend
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import doobie.util.transactor.Transactor
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.*
import io.leisuremeta.chain.lmscan.agent.service.*
import io.leisuremeta.chain.lmscan.agent.service.RequestServiceApp
import io.leisuremeta.chain.lmscan.agent.apps.DataStoreApp
import io.leisuremeta.chain.lmscan.agent.apps.BalanceApp
import io.leisuremeta.chain.lmscan.agent.apps.NftApp
import cats.effect.std.Queue
import io.leisuremeta.chain.api.model.Signed.TxHash
import io.leisuremeta.chain.api.model.TransactionWithResult
import io.leisuremeta.chain.api.model.Transaction.TokenTx
import io.leisuremeta.chain.api.model.Account

object ScanAgentResource:
  def transactorBuilder[F[_]: Async](conf: DBConfig) =
    for
      conf <- Resource.pure:
        val config = new HikariConfig()
        config.setDriverClassName(conf.driver)
        config.setJdbcUrl(conf.url)
        config
      xa <- HikariTransactor.fromHikariConfig[F](conf)
    yield xa

  def build[F[_]: Async](conf: ScanAgentConfig): Resource[
    EitherT[F, String, *],
    (
        Transactor[F],
        Transactor[F],
        SttpBackend[F, Any],
    ),
  ] =
    for
      postgres <- transactorBuilder[F](conf.remote).mapK(
        EitherT.liftK[F, String],
      )
      sqlite <- transactorBuilder[F](conf.local).mapK(EitherT.liftK[F, String])
      sttp <- HttpClientFs2Backend.resource[F]().mapK(EitherT.liftK[F, String])
    yield (postgres, sqlite, sttp)

object LoopCheckerApp:
  def build[F[_]: Async](
      remote: RemoteStoreApp[F],
      local: LocalStoreApp[F],
      client: RequestServiceApp[F],
      base: String,
  ): F[(DataStoreApp[F], BalanceApp[F], NftApp[F])] =
    val nftQ = Queue.bounded[F, (TxHash, TokenTx, Account)](1000)
    val balQ = Queue.bounded[F, (TxHash, TransactionWithResult)](1000)
    for
      qa <- nftQ
      qb <- balQ
      storeApp <- Async[F].pure:
        DataStoreApp.build(qa, qb)(remote, client, base)
      balApp <- Async[F].pure:
        BalanceApp.build(qb)(remote, local)
      nftApp <- Async[F].pure:
        NftApp.build(qa)(remote, client)
    yield (storeApp, balApp, nftApp)
  def run[F[_]: Async](
      remote: RemoteStoreApp[F],
      local: LocalStoreApp[F],
      client: RequestServiceApp[F],
      base: String,
  ): F[Unit] =
    for
      (a, b, c) <- build[F](remote, local, client, base)
      _         <- a.run &> b.run &> c.run
    yield ()
