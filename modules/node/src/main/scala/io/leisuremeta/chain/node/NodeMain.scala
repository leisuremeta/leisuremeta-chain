package io.leisuremeta.chain
package node

import java.nio.file.{Path, Paths}

import cats.effect.{ExitCode, IO, IOApp}
//import cats.effect.unsafe.implicits.global
import com.typesafe.config.{Config, ConfigFactory}

import api.{LeisureMetaChainApi as Api}
import api.model.{Block, Signed}
import lib.codec.byte.ByteCodec
import lib.crypto.Hash
import lib.datatype.{BigNat, UInt256Bytes}
import repository.BlockRepository
import store.*
import store.interpreter.StoreIndexSwayInterpreter
import cats.effect.unsafe.IORuntime

object NodeMain extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =

    def sway[K: ByteCodec, V: ByteCodec](dir: Path): IO[StoreIndex[IO, K, V]] =
      StoreIndexSwayInterpreter[K, V](dir)

    def getBlockRepo: IO[BlockRepository[IO]] = for
      bestBlockKVStore <- sway[UInt256Bytes, Block.Header](
        Paths.get("sway", "block", "best"),
      )
      given SingleValueStore[IO, Block.Header] = SingleValueStore
        .fromKeyValueStore[IO, Block.Header](bestBlockKVStore)
      given StoreIndex[IO, Block.BlockHash, Block] <- sway[Hash.Value[
        Block,
      ], Block](Paths.get("sway", "block"))
      given StoreIndex[IO, BigNat, Block.BlockHash] <- sway[
        BigNat,
        Block.BlockHash,
      ](Paths.get("sway", "block", "number"))
      given StoreIndex[IO, Signed.TxHash, Block.BlockHash] <- sway[
        Signed.TxHash,
        Block.BlockHash,
      ](Paths.get("sway", "block", "tx"))
    yield BlockRepository.fromStores[IO]

    val getConfig: IO[Config] = IO.blocking(ConfigFactory.load)
    NodeConfig.load[IO](getConfig).value.flatMap {
      case Right(config) =>
        for
          given BlockRepository[IO] <- getBlockRepo
          exitcode <- NodeApp[IO](config).resource.useForever
            .as(ExitCode.Success)
        yield exitcode
      case Left(err) =>
        IO(println(err)).as(ExitCode.Error)
    }
