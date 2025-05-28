package io.leisuremeta.chain
package lmscan.agent

import cats.effect.*
import service.RequestService
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.agent.service.StoreService
import scribe.file.*
import scribe.format.*
import io.leisuremeta.chain.lmscan.agent.apps.SummaryStoreApp

object ScanAgentMain extends IOApp:
  scribe.Logger.root
    .withHandler(
      writer = FileWriter(
        "logs" / ("agent-" % year % "-" % month % "-" % day % ".log"),
      ),
      formatter =
        formatter"[$threadName] $positionAbbreviated - $messages$newLine",
      minimumLevel = Some(scribe.Level.Error),
    )
    .replace()
  def run(args: List[String]): IO[ExitCode] =
    val conf = ScanAgentConfig.load
    ScanAgentResource
      .build[IO](conf)
      .use: (post, sqlite, server) =>
        val client = RequestService.build[IO](server)
        val remote = StoreService.buildRemote(post)
        val local  = StoreService.buildLocal(sqlite)
        val summary = SummaryStoreApp.build[IO](conf.market, conf.es)(
          remote,
          client,
        )
        EitherT.liftF:
          summary.run &> LoopCheckerApp.run(remote, local, client, conf.base)
      .value
      .as(ExitCode.Success)
