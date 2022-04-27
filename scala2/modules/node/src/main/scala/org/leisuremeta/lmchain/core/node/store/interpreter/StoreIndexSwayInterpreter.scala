package org.leisuremeta.lmchain.core
package node
package store
package interpreter

import java.nio.file.Path

import scala.concurrent.ExecutionContext

import cats.Monad
import cats.data.EitherT
import cats.effect.{ContextShift, IO}
import cats.implicits._

import scodec.bits.ByteVector
import swaydb.Map
import swaydb.data.order.KeyOrder
import swaydb.data.slice.Slice
import swaydb.serializers.Default.ByteArraySerializer

import codec.byte.{ByteCodec, ByteDecoder, ByteEncoder, DecodeResult}
import datatype.BigNat
import failure.DecodingFailure

@SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
class StoreIndexSwayInterpreter[K, V: ByteCodec](
    map: Map[K, Array[Byte], Nothing, IO],
    dir: Path,
)(implicit cs: ContextShift[IO])
    extends StoreIndex[IO, K, V] {

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
  def apply[K: ByteCodec, V: ByteCodec](dir: Path)(implicit
      cs: ContextShift[IO],
      ec: ExecutionContext,
  ): StoreIndexSwayInterpreter[K, V] = {

    implicit val b: swaydb.Bag[IO] = swaydb.cats.effect.Bag(cs, ec)

    scribe.debug(s"===> Generating mapIO with path $dir")

    val map: Map[K, Array[Byte], Nothing, IO] =
      swaydb.persistent.Map[K, Array[Byte], Nothing, IO](dir).unsafeRunSync()
    new StoreIndexSwayInterpreter[K, V](map, dir)
  }

  def ensureNoRemainder[A](
      decoded: DecodeResult[A],
      msg: String,
  ): Either[DecodingFailure, A] =
    Either.cond(decoded.remainder.isEmpty, decoded.value, DecodingFailure(msg))

  @SuppressWarnings(Array("org.wartremover.warts.ImplicitParameter"))
  def reverseBignatStoreIndex[A: ByteCodec](dir: Path)(implicit
      cs: ContextShift[IO],
      ec: ExecutionContext,
  ): StoreIndexSwayInterpreter[BigNat, A] = {
    implicit val k: KeyOrder[Slice[Byte]] = KeyOrder.reverseLexicographic
    implicit val b: swaydb.Bag[IO]        = swaydb.cats.effect.Bag(cs, ec)
    val map = swaydb.persistent
      .Map[BigNat, Array[Byte], Nothing, IO](dir)
      .unsafeRunSync()
    new StoreIndexSwayInterpreter[BigNat, A](map, dir)
  }
}
