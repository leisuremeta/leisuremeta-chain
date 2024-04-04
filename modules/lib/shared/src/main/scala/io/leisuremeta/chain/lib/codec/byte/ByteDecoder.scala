package io.leisuremeta.chain.lib
package codec.byte

import java.time.Instant

import scala.compiletime.{erasedValue, summonInline}
import scala.deriving.Mirror
import scala.reflect.{ClassTag, classTag}

import cats.syntax.eq.catsSyntaxEq
import cats.syntax.either.*

import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import scodec.bits.ByteVector

import failure.DecodingFailure

trait ByteDecoder[A]:
  def decode(bytes: ByteVector): Either[DecodingFailure, DecodeResult[A]]

  def map[B](f: A => B): ByteDecoder[B] = bytes =>
    decode(bytes).map { case DecodeResult(a, remainder) =>
      DecodeResult(f(a), remainder)
    }

  def emap[B](f: A => Either[DecodingFailure, B]): ByteDecoder[B] = bytes =>
    for
      decoded   <- decode(bytes)
      converted <- f(decoded.value)
    yield DecodeResult(converted, decoded.remainder)

  def flatMap[B](f: A => ByteDecoder[B]): ByteDecoder[B] = bytes =>
    decode(bytes).flatMap { case DecodeResult(a, remainder) =>
      f(a).decode(remainder)
    }

  def widen[AA >: A]: ByteDecoder[AA] = map(identity)

final case class DecodeResult[+A](value: A, remainder: ByteVector)

object ByteDecoder:
  def apply[A: ByteDecoder]: ByteDecoder[A] = summon

  object ops:
    extension (bytes: ByteVector)
      def to[A: ByteDecoder]: Either[DecodingFailure, A] = for
        result <- ByteDecoder[A].decode(bytes)
        DecodeResult(a, r) = result
        _ <- Either.cond(
          r.isEmpty,
          (),
          DecodingFailure(s"non empty remainder: $r"),
        )
      yield a

  private def decoderProduct[A](
      p: Mirror.ProductOf[A],
      elems: => List[ByteDecoder[?]],
  ): ByteDecoder[A] = (bytes: ByteVector) =>

    def reverse(tuple: Tuple): Tuple =
      @SuppressWarnings(Array("org.wartremover.warts.Any"))
      @annotation.tailrec
      def loop(tuple: Tuple, acc: Tuple): Tuple = tuple match
        case _: EmptyTuple => acc
        case t *: ts       => loop(ts, t *: acc)
      loop(tuple, EmptyTuple)

    @annotation.tailrec
    def loop(
        elems: List[ByteDecoder[?]],
        bytes: ByteVector,
        acc: Tuple,
    ): Either[DecodingFailure, DecodeResult[A]] = elems match
      case Nil =>
        (DecodeResult(p.fromProduct(reverse(acc)), bytes))
          .asRight[DecodingFailure]
      case decoder :: rest =>
        scribe.debug(s"Decoder: $decoder")
        scribe.debug(s"Bytes to decode: $bytes")
        decoder.decode(bytes) match
          case Left(failure) => failure.asLeft[DecodeResult[A]]
          case Right(DecodeResult(value, remainder)) =>
            scribe.debug(s"Decoded: $value")
            loop(rest, remainder, value *: acc)
    loop(elems, bytes, EmptyTuple)

  inline def summonAll[T <: Tuple]: List[ByteDecoder[?]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => summonInline[ByteDecoder[t]] :: summonAll[ts]

  inline given derived[T](using p: Mirror.ProductOf[T]): ByteDecoder[T] =
    lazy val elemInstances: List[ByteDecoder[?]] =
      summonAll[p.MirroredElemTypes]
    decoderProduct(p, elemInstances)

  given unitByteDecoder: ByteDecoder[Unit] = bytes =>
    Right[DecodingFailure, DecodeResult[Unit]](
      DecodeResult((), bytes),
    )

  type BigNat = BigInt Refined NonNegative

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def unsafeFromBigInt(n: BigInt): BigNat = refineV[NonNegative](n) match
    case Right(nat) => nat
    case Left(e)    => throw new Exception(e)

  def fromFixedSizeBytes[T: ClassTag](
      size: Long,
  )(f: ByteVector => T): ByteDecoder[T] = bytes =>
    Either.cond(
      bytes.size >= size,
      bytes splitAt size match
        case (front, back) => DecodeResult(f(front), back)
      ,
      DecodingFailure(
        s"Too shord bytes to decode ${classTag[T]}; required $size bytes, but receiced ${bytes.size} bytes: $bytes",
      ),
    )

//  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
//  given ByteDecoder[UInt256BigInt] = fromFixedSizeBytes(32) { bytes =>
//    UInt256.from(BigInt(1, bytes.toArray)).toOption.get
//  }
//
//  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
//  given ByteDecoder[UInt256Bytes] = fromFixedSizeBytes(32) {
//    UInt256.from(_).toOption.get
//  }

  given byteDecoder: ByteDecoder[Byte] = fromFixedSizeBytes(1)(_.toByte())

  given longDecoder: ByteDecoder[Long] = fromFixedSizeBytes(8)(_.toLong())

  given instantDecoder: ByteDecoder[Instant] =
    ByteDecoder[Long] map Instant.ofEpochMilli

  given bignatByteDecoder: ByteDecoder[BigNat] = bytes =>
    Either.cond(bytes.nonEmpty, bytes, DecodingFailure("Empty bytes")).flatMap {
      nonEmptyBytes =>
        val head: Int        = nonEmptyBytes.head & 0xff
        val tail: ByteVector = nonEmptyBytes.tail
        if head <= 0x80 then
          Right[DecodingFailure, DecodeResult[BigNat]](
            DecodeResult(unsafeFromBigInt(BigInt(head)), tail),
          )
        else if head <= 0xf8 then
          val size = head - 0x80
          if tail.size < size then
            Left[DecodingFailure, DecodeResult[BigNat]](
              DecodingFailure(
                s"required byte size $size, but $tail",
              ),
            )
          else
            val (front, back) = tail.splitAt(size.toLong)
            Right[DecodingFailure, DecodeResult[BigNat]](
              DecodeResult(unsafeFromBigInt(BigInt(1, front.toArray)), back),
            )
        else
          val sizeOfNumber = head - 0xf8 + 1
          if tail.size < sizeOfNumber then
            Left[DecodingFailure, DecodeResult[BigNat]](
              DecodingFailure(
                s"required byte size $sizeOfNumber, but $tail",
              ),
            )
          else
            val (sizeBytes, data) = tail.splitAt(sizeOfNumber.toLong)
            val size              = BigInt(1, sizeBytes.toArray).toLong

            if data.size < size then
              Left[DecodingFailure, DecodeResult[BigNat]](
                DecodingFailure(
                  s"required byte size $size, but $data",
                ),
              )
            else
              val (front, back) = data.splitAt(size)
              Right[DecodingFailure, DecodeResult[BigNat]](
                DecodeResult(unsafeFromBigInt(BigInt(1, front.toArray)), back),
              )
    }

  given bigintByteDecoder: ByteDecoder[BigInt] = ByteDecoder[BigNat].map{
    case x if x % 2 === 0 => x / 2
    case x => (x - 1) / (-2)
  }

  def sizedListDecoder[A: ByteDecoder](size: BigNat): ByteDecoder[List[A]] =
    bytes =>
      @annotation.tailrec
      def loop(
          bytes: ByteVector,
          count: BigInt,
          acc: List[A],
      ): Either[DecodingFailure, DecodeResult[List[A]]] =
        if count === BigInt(0) then
          Right[DecodingFailure, DecodeResult[List[A]]](
            DecodeResult(acc.reverse, bytes),
          )
        else
          ByteDecoder[A].decode(bytes) match
            case Left(failure) =>
              Left[DecodingFailure, DecodeResult[List[A]]](failure)
            case Right(DecodeResult(value, remainder)) =>
              loop(remainder, count - 1, value :: acc)
      loop(bytes, size, Nil)

  given mapByteDecoder[K: ByteDecoder, V: ByteDecoder]: ByteDecoder[Map[K, V]] =
    bignatByteDecoder flatMap sizedListDecoder[(K, V)] map (_.toMap)

  given optionByteDecoder[A: ByteDecoder]: ByteDecoder[Option[A]] =
    bignatByteDecoder flatMap sizedListDecoder[A] map (_.headOption)

  given setByteDecoder[A: ByteDecoder]: ByteDecoder[Set[A]] =
    bignatByteDecoder flatMap sizedListDecoder[A] map (_.toSet)

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  given seqByteDecoder[A: ByteDecoder]: ByteDecoder[Seq[A]] =
    bignatByteDecoder flatMap sizedListDecoder[A] map (_.asInstanceOf[Seq[A]])
    