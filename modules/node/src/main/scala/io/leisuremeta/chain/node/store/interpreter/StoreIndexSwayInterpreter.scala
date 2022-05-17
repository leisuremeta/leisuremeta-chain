package io.leisuremeta.chain
package node
package store
package interpreter

import java.nio.file.Path

import scala.concurrent.ExecutionContext

import cats.Monad
import cats.data.EitherT
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.implicits._

import scodec.bits.ByteVector
import swaydb.Map
import swaydb.data.order.KeyOrder
import swaydb.data.slice.Slice
import swaydb.serializers.Serializer
import swaydb.serializers.Default.ByteArraySerializer

import lib.codec.byte.{ByteCodec, ByteDecoder, ByteEncoder, DecodeResult}
import lib.datatype.BigNat
import lib.failure.DecodingFailure

import Bag.given

@SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
class StoreIndexSwayInterpreter[K, V: ByteCodec](
    map: Map[K, Array[Byte], Nothing, IO],
    dir: Path,
) extends StoreIndex[IO, K, V] {

  def get(key: K): EitherT[IO, DecodingFailure, Option[V]] = for {
    _ <- EitherT.pure[IO, DecodingFailure](
      scribe.debug(s"===> $dir: Geting with key: $key")
    )
    arrayOption <- EitherT.right(map.get(key))
    _ <- EitherT.pure[IO, DecodingFailure](
      scribe.debug(
        s"===> $dir: Got value: ${arrayOption.map(ByteVector.view).map(_.toHex)}"
      )
    )
    decodeResult <- arrayOption.traverse { array =>
      EitherT.fromEither[IO](
        ByteDecoder[V].decode(ByteVector.view(array))
      )
    }
  } yield decodeResult.map(_.value)

  def put(key: K, value: V): IO[Unit] = for {
    _ <- Monad[IO].pure(scribe.debug(s"===> $dir: Putting $key -> $value"))
    _ <- map.put(key, ByteEncoder[V].encode(value).toArray)
  } yield ()

  def remove(key: K): IO[Unit] = map.remove(key).map(_ => ())

  def from(
      key: K,
      offset: Int,
      limit: Int,
  ): EitherT[IO, DecodingFailure, List[(K, V)]] = EitherT(
    map
      .fromOrAfter(key)
      .stream
      .drop(offset)
      .take(limit)
      .materialize
      .map(
        _.toList.traverse { case (key, valueArray) =>
          for {
            valueDecoded <- (ByteDecoder[V].decode(ByteVector view valueArray))
            value <- StoreIndexSwayInterpreter.ensureNoRemainder(
              valueDecoded,
              s"Value bytes decoded with nonempty reminder: $valueDecoded",
            )
          } yield (key, value)
        }
      )
  )
}

object StoreIndexSwayInterpreter {

  @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
  def apply[K: ByteCodec, V: ByteCodec](dir: Path)(using IORuntime): StoreIndexSwayInterpreter[K, V] = {

    scribe.debug(s"===> Generating mapIO with path $dir")
    implicit val k: KeyOrder[Slice[Byte]] = KeyOrder.default
    given KeyOrder[K] = null
    given ExecutionContext = swaydb.configs.level.DefaultExecutionContext.compactionEC

    val map: Map[K, Array[Byte], Nothing, IO] = swaydb.persistent.Map[K, Array[Byte], Nothing, IO](dir).unsafeRunSync()

    new StoreIndexSwayInterpreter[K, V](map, dir)
  }

  def ensureNoRemainder[A](
      decoded: DecodeResult[A],
      msg: String,
  ): Either[DecodingFailure, A] =
    Either.cond(decoded.remainder.isEmpty, decoded.value, DecodingFailure(msg))

  given scala.reflect.ClassTag[Nothing] = scala.reflect.Manifest.Nothing
  given swaydb.data.sequencer.Sequencer[IO] = null
  given swaydb.core.build.BuildValidator = swaydb.core.build.BuildValidator.DisallowOlderVersions(swaydb.data.DataType.Map)


  @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
  def reverseBignatStoreIndex[A: ByteCodec](dir: Path)(using IORuntime): StoreIndexSwayInterpreter[BigNat, A] = {
    implicit val k: KeyOrder[Slice[Byte]] = KeyOrder.reverseLexicographic
    val map = swaydb.persistent.Map[BigNat, Array[Byte], Nothing, IO](dir).unsafeRunSync()
    new StoreIndexSwayInterpreter[BigNat, A](map, dir)
  }

  
  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  implicit def byteCodecToSerialize[A: ByteCodec]: Serializer[A] =
    new Serializer[A] {
      override def write(data: A): Slice[Byte] =
        Slice[Byte](ByteEncoder[A].encode(data).toArray)
      override def read(data: Slice[Byte]): A =
        ByteDecoder[A].decode(ByteVector view data.toArray) match {
          case Right(DecodeResult(value, remainder)) if remainder.isEmpty =>
            value
          case anything =>
            throw new Exception(s"Fail to decode $data: $anything")
        }
    }

}
