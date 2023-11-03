package io.leisuremeta.chain
package node
package store
package interpreter

import java.nio.file.Path

import scala.concurrent.ExecutionContext

import cats.Monad
import cats.data.EitherT
import cats.effect.{IO, Resource}

import scodec.bits.ByteVector
import io.lettuce.core._

import lib.codec.byte.{ByteCodec, ByteDecoder, ByteEncoder, DecodeResult}
import lib.failure.DecodingFailure
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.codec.RedisCodec
import java.nio.ByteBuffer
import scala.util.Try
import io.leisuremeta.chain.lib.datatype.Utf8
import scala.util.Success
import io.leisuremeta.chain.lib.datatype.UInt256
import io.leisuremeta.chain.node.NodeConfig.RedisConfig

@SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
class RedisInterpreter[K, V: ByteCodec](
    cmds: RedisAsyncCommands[K, V],
    dir: RedisPath,
) extends KeyValueStore[IO, K, V]:
  def get(key: K): EitherT[IO, DecodingFailure, Option[V]] =
    for
      _ <- EitherT.pure[IO, DecodingFailure]:
        scribe.debug(s"===> $dir: Geting with key: $key")
      v <- Try[V](cmds.get(key).get()) match
        case Success(value) if (value != null) => EitherT.right(IO.pure(Some(value)))
        case _ => EitherT.right(IO.pure(None))
      _ <- EitherT.pure[IO, DecodingFailure]:
        scribe.debug(s"===> $dir: Got value: ${v}")
    yield v

  def put(key: K, value: V): IO[Unit] = 
    for
      _ <- Monad[IO].pure(scribe.debug(s"===> $dir: Putting $key -> $value"))
      _ = cmds.set(key, value)
    yield ()

  def remove(key: K): IO[Unit] = 
    for
      _ <- IO.unit
      _ = cmds.del(key)
    yield ()

case class RedisPath(path: String, db: Int):
  def toByteVector = Utf8.unsafeFrom(path).bytes

object RedisInterpreter:
  def apply[K: ByteCodec, V: ByteCodec](config: RedisConfig, dir: RedisPath): Resource[IO, RedisInterpreter[K, V]] = 
    object CodecImpl extends RedisCodec[K, V]:
      @SuppressWarnings(Array("org.wartremover.warts.Throw"))
      def decodeKey(bytes: ByteBuffer): K =
        ByteCodec[K].decode(ByteVector(bytes)) match
          case Right(DecodeResult(v, r)) if r.isEmpty => v
          case _ => throw new Exception(s"Fail to decode $bytes")
      @SuppressWarnings(Array("org.wartremover.warts.Throw"))
      def decodeValue(bytes: ByteBuffer): V =
        ByteCodec[V].decode(ByteVector(bytes)) match
          case Right(DecodeResult(v, r)) if r.isEmpty => v
          case _ => throw new Exception(s"Fail to decode $bytes")
      def encodeKey(key: K): ByteBuffer = 
        if (key != UInt256.EmptyBytes) (dir.toByteVector ++ ByteCodec[K].encode(key)).toByteBuffer
        else dir.toByteVector.toByteBuffer
      def encodeValue(value: V): ByteBuffer =
        ByteCodec[V].encode(value).toByteBuffer

    val uri = RedisURI.Builder.redis(config.host, config.port).withDatabase(dir.db).build()
    val client = IO(RedisClient.create(uri))

    Resource
      .make(client)(c => IO(c.close()))
      .map(c => new RedisInterpreter[K, V](c.connect(CodecImpl).async(), dir))
