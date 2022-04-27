package org.leisuremeta.lmchain.core
package model

import scala.math.Ordering

import cats.Eq
import cats.implicits._

import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import scodec.bits.ByteVector

import datatype.BigNat
import failure.EncodingFailure
import codec.byte.{ByteDecoder, ByteEncoder}
import ByteEncoder.ops._

sealed trait Account {
  override def toString: String = this match {
    case Account.Named(name)      => name.toString()
    case Account.Unnamed(address) => s"0x${address.toString()}"
  }
}

object Account {

  final case class Named(
      name: Name
  ) extends Account

  final case class Unnamed(
      address: Address
  ) extends Account

  final case class Name private[model] (bytes: ByteVector) extends AnyVal {
    override def toString: String =
      bytes.decodeUtf8.getOrElse(s"invalid utf8: $bytes")
  }

  object Name {
    def from(string: String): Either[EncodingFailure, Name] = {
      ByteVector.encodeUtf8(string) match {
        case Right(bytes) if bytes.nonEmpty => Right(Name(bytes))
        case Left(e)                        => Left(EncodingFailure(e.getMessage()))
        case _                              => Left(EncodingFailure(s"Empty Name: $string"))
      }
    }

    def unsafeFromBytes(bytes: ByteVector): Name = new Name(bytes)

    implicit val nameOrdering: Ordering[Name] = Ordering.by(_.bytes)

    implicit val nameEq: Eq[Name] = Eq.fromUniversalEquals

    implicit val circeNameDecoder: Decoder[Name] = Decoder.decodeString.emap {
      Name.from(_).left.map(_.msg)
    }
    implicit val circeNameEncoder: Encoder[Name] =
      Encoder.encodeString.contramap(_.toString())

    implicit val circeNameKeyDecoder: KeyDecoder[Name] =
      KeyDecoder.instance(Name.from(_).toOption)
    implicit val circeNameKeyEncoder: KeyEncoder[Name] =
      KeyEncoder.instance(_.toString)
  }

  implicit val accountEncoder: ByteEncoder[Account] = {
    case Named(name) =>
      val nameSizeBytes = BigNat.unsafeFromLong(name.bytes.size).toBytes
      val nameBytes     = name.bytes
      nameSizeBytes ++ nameBytes
    case Unnamed(address) =>
      BigNat.Zero.toBytes ++ address.toBytes
  }

  implicit val accountDecoder: ByteDecoder[Account] =
    ByteDecoder[BigNat].flatMap {
      case size if size.value === BigInt(0) =>
        ByteDecoder[Address].map(Unnamed(_))
      case size =>
        for {
          name <- ByteDecoder.fromFixedSizeBytes(size.value.toLong)(
            Name.unsafeFromBytes
          )
        } yield Named(name)
    }

  implicit val circeAccountDecoder: Decoder[Account] = {
    val unnamedDecoder: Decoder[Account] = Decoder[Address].map(Unnamed(_))
    val namedDecoder: Decoder[Account]   = Decoder[Name].map(Named(_))
    unnamedDecoder `or` namedDecoder
  }

  implicit val circeAccountEncoder: Encoder[Account] = Encoder.instance {
    case Unnamed(address) => address.asJson
    case Named(name)      => name.asJson
  }

  implicit val circeAccountKeyDecoder: KeyDecoder[Account] =
    KeyDecoder.instance { (s: String) =>
      def unnamedOption: Option[Account] =
        KeyDecoder[Address].apply(s).map(Unnamed(_))
      def namedOption: Option[Account] =
        KeyDecoder[Name].apply(s).map(Named(_))
      unnamedOption `orElse` namedOption
    }

  implicit val circeAccountKeyEncoder: KeyEncoder[Account] =
    KeyEncoder.instance {
      case Unnamed(address) => s"0x${address.toString}"
      case Named(name)      => name.toString
    }
}
