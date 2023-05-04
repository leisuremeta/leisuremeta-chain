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
import cats.instances.queue



object NodeWatchService:
  def nodeConfig[F[_]: Async]: F[Either[Throwable, NodeConfig]] = Async[F].blocking {
    // val path = Paths.get("/Users/jichangho/playnomm/leisuremeta-chain/migration-node.json")
    // val path = Paths.get("/Users/user/playnomm/source_code/leisuremeta-chain/migration-node.json")
    val path = Paths.get("/home/rocky/nodeproxy/migration-node.json")
    
      for 
        json <- Try(Files.readAllLines(path).asScala.mkString("\n")).toEither
        nodeConfig <- decode[NodeConfig](json)
      yield nodeConfig
  }

  def newNodeWatchLoop[F[_]: Async](
    apiService: InternalApiService[F], 
    blcUrls:    Ref[F, List[String]],
    blocker:    Ref[F, Boolean],
    queue:      PostTxQueue[F]
  ): F[Unit] = 
    def loop: F[Option[Unit]] = 
      nodeConfig.flatMap { nodeConfigEither =>
        scribe.info(s"newNodeWatchLoop")
        nodeConfigEither match 
          case Right(nodeConfig) => 
            (nodeConfig.oldNodeAddress, nodeConfig.newNodeAddress) match 
              case (oldNodeAddress, Some(newNodeAddress)) if !newNodeAddress.trim.isEmpty => 
                scribe.info(s"nodeConfig: $nodeConfig")
                blcUrls.set(List(nodeConfig.oldNodeAddress)) 
                *> NodeBalancer(apiService, blocker, blcUrls, nodeConfig, queue).run() 
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
      case Some(_) => Async[F].sleep(5.seconds) >> newNodeWatchLoop(apiService, blcUrls, blocker, queue)
      case None    => Async[F].unit
    }
  
  def startOnNew[F[_]: Async](
    apiService: InternalApiService[F], 
    blcUrls:    Ref[F, List[String]], 
    blocker:    Ref[F, Boolean],
    queue:      PostTxQueue[F]
  ): F[Unit] = 
    Async[F].executionContext.flatMap { executionContext =>
      Async[F].startOn(
        newNodeWatchLoop(apiService, blcUrls, blocker, queue),
        executionContext
      ) *> Async[F].unit
    } 

  def waitTerminateSig[F[_]: Async]: F[Unit] =
    def loop: F[Unit] =
      nodeConfig.flatMap { nodeCfgEither =>
        nodeCfgEither match
          case Right(nodeConfig) =>
            nodeConfig.newNodeAddress match
              case Some(address) if address.isBlank() => 
                        Async[F].delay(scribe.info("마이그레이션 종료.")) 
                         >> Async[F].unit
              case _ => Async[F].delay(scribe.info("종료 시그널 기다리는 중..")) 
                         >> Async[F].sleep(10.second) 
                         >> loop
          case Left(error) =>
            scribe.error(s"Error decoding Node Config: $error\nreconfigure migration-node.json file.")
            Async[F].sleep(5.second)
            loop
      }
    loop
