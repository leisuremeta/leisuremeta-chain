package io.leisuremeta.chain
package node
package store
package interpreter

import java.nio.file.Path

import cats.data.EitherT
import cats.effect.{IO, Resource}

import lib.codec.byte.ByteCodec
import lib.failure.DecodingFailure
import io.leisuremeta.chain.node.NodeConfig.RedisConfig
import java.nio.file.Paths

@SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
class MultiInterpreter[K, V: ByteCodec](
    redis: RedisInterpreter[K, V],
    sway: SwayInterpreter[K, V],
) extends KeyValueStore[IO, K, V]:
  def get(key: K): EitherT[IO, DecodingFailure, Option[V]] = 
    for 
      rRes <- redis.get(key)
      res <- rRes match
        case Some(v) => EitherT.pure[IO, DecodingFailure](Some(v))
        case _ => sway.get(key)
    yield res

  def put(key: K, value: V): IO[Unit] = 
    for 
      _ <- redis.put(key, value)
      _ <- sway.put(key, value)
    yield ()
  def remove(key: K): IO[Unit] = 
    for 
      _ <- redis.remove(key)
      _ <- sway.remove(key)
    yield ()

object MultiInterpreter:
  def apply[K: ByteCodec, V: ByteCodec](config: RedisConfig, dir: InterpreterTarget): Resource[IO, MultiInterpreter[K, V]] = 
    for
      redis <- RedisInterpreter[K, V](config, dir.r)
      sway <- SwayInterpreter[K, V](dir.s)
      res = new MultiInterpreter(redis, sway)
    yield res

case class InterpreterTarget(r: RedisPath, s: Path)
object InterpreterTarget:
  val BEST_NUM = InterpreterTarget(RedisPath("node:best", 0), Paths.get("sway", "block", "best"))
  val BLOCK = InterpreterTarget(RedisPath("node:blc:", 0), Paths.get("sway", "block"))
  val BLOCK_NUM = InterpreterTarget(RedisPath("node:blc_num:", 0), Paths.get("sway", "block", "number"))
  val TX_BLOCK = InterpreterTarget(RedisPath("node:tx_blc:", 0), Paths.get("sway", "block", "tx"))
  val MERKLE_TRIE = InterpreterTarget(RedisPath("node:trie:", 0), Paths.get("sway", "state"))
  val TX = InterpreterTarget(RedisPath("node:tx:", 0), Paths.get("sway", "transaction"))
