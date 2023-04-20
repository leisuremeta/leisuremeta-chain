package io.leisuremeta.chain.node.proxy
package service

import cats.effect.Ref
import cats.implicits.toFlatMapOps
import cats.syntax.flatMap.toFlatMapOps
import cats.effect.Async
import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.util.Try
import scala.jdk.CollectionConverters.*
import model.NodeConfig
import io.circe.generic.auto.*
import io.circe.parser.decode
import scala.concurrent.duration.*
import cats.implicits.catsSyntaxFlatMapOps
import cats.syntax.all.toFunctorOps
import cats.syntax.apply.catsSyntaxApply
import cats.syntax.functor._
import cats.implicits._



object NodeWatchService:
  def defaultNodeCfg[F[_]: Async]: F[Either[Throwable, NodeConfig]] = Async[F].blocking {
    val path = Paths.get("/Users/user/playnomm/source_code/leisuremeta-chain/migration-node.json")
      for 
        json <- Try(Files.readAllLines(path).asScala.mkString("\n")).toEither
        nodeConfig <- decode[NodeConfig](json)
      yield nodeConfig
  }

  def newNodeWatchLoop[F[_]: Async](apiService: InternalApiService[F], blocker: Ref[F, Boolean],  blcUrls: Ref[F, List[String]]): F[Unit] = 
    def loop: F[Option[Unit]] = 
      defaultNodeCfg.flatMap { nodeConfigEither =>
        scribe.info(s"newNodeWatchLoop")
        nodeConfigEither match 
          case Right(nodeConfig) => 
            (nodeConfig.oldNodeAddress, nodeConfig.newNodeAddress) match 
              case (oldNodeAddress, Some(newNodeAddress)) if !newNodeAddress.trim.isEmpty => 
                scribe.info(s"nodeConfig: $nodeConfig")
                blcUrls.set(List(nodeConfig.oldNodeAddress, newNodeAddress)) 
                *> NodeBalancer(apiService, blocker, nodeConfig).run() 
                *> Async[F].pure(Some(()))
              case (oldNodeAddress, _) => // newNodeAddress is None | isEmpty
                for 
                  urls <- blcUrls.get
                  _    <- if urls.isEmpty then blcUrls.set(List(oldNodeAddress))
                          else if !urls.head.equals(oldNodeAddress) then blcUrls.set(List(oldNodeAddress))
                          else Async[F].unit
                yield Some(())
          case Left(error) =>
            scribe.error(s"Error decoding Node Config: $error\nreconfigure migration-node.json file.")
            Async[F].pure(Some(()))
      }

    loop.flatMap {
      case Some(_) => Async[F].sleep(5.seconds) >> newNodeWatchLoop(apiService, blocker, blcUrls)
      case None    => Async[F].unit
    }
  
  def startOnNew[F[_]: Async](apiService: InternalApiService[F], blocker: Ref[F, Boolean], blcUrls: Ref[F, List[String]]): F[Unit] = 
    Async[F].executionContext.flatMap { executionContext =>
      Async[F].startOn(
        newNodeWatchLoop(apiService, blocker, blcUrls),
        executionContext
      ) *> Async[F].unit
    } 
    
