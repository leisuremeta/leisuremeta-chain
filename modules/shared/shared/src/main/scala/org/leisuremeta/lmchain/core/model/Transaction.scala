package org.leisuremeta.lmchain.core
package model

import java.time.Instant

import cats.kernel.Eq

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.auto._
import io.circe.refined._
import io.circe.syntax._
import scodec.bits.ByteVector
import shapeless.tag.@@

import codec.byte.{ByteDecoder, ByteEncoder}
import codec.circe._
import crypto.Hash
import datatype.{BigNat, UInt256Bytes, Utf8}
import failure.DecodingFailure
import Transaction.Name._
import Transaction.Token._

sealed trait Transaction {
  def networkId: NetworkId
  def createdAt: Instant
  def inputs: Set[Transaction.Input] = this match {
    case _: CreateName  => Set.empty
    case t: UpdateName  => Set(Transaction.Input.Name(t.name))
    case t: DeleteName  => Set(Transaction.Input.Name(t.name))
    case _: DefineToken => Set.empty
    case t: TransferAdmin =>
      Set(Transaction.Input.TokenDef(t.definitionId))
    case _: MintToken => Set.empty
    case t: TransferToken =>
      t.inputTxs.map(Transaction.Input.Tx(_, t.divisionIndex)).toSet
    case t: CombineDivision => t.inputTxs.toSet
    case t: DivideToken     => Set(Transaction.Input.Tx(t.inputTx, None))
  }
}

object Transaction {

  type TxHash = Hash.Value[Transaction]

  sealed trait Input
  object Input {
    final case class Name(name: Account.Name)             extends Input
    final case class TokenDef(definitionId: UInt256Bytes) extends Input
    final case class Tx(txHash: Signed.TxHash, divisionIndex: Option[BigNat])
        extends Input

    implicit val eq: Eq[Input] = Eq.fromUniversalEquals
  }

  sealed trait Name extends Transaction {
    def name: Account.Name
  }
  object Name {
    final case class CreateName(
        networkId: NetworkId,
        createdAt: Instant,
        name: Account.Name,
        state: NameState,
    ) extends Name

    final case class UpdateName(
        networkId: NetworkId,
        createdAt: Instant,
        name: Account.Name,
        state: NameState,
    ) extends Name

    final case class DeleteName(
        networkId: NetworkId,
        createdAt: Instant,
        name: Account.Name,
    ) extends Name
  }

  sealed trait Token extends Transaction
  object Token {
    final case class DefineToken(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: DefinitionId,
        name: Utf8,
        symbol: Utf8,
        divisionSize: BigNat,
        data: ByteVector,
    ) extends Token

    final case class TransferAdmin(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: DefinitionId,
        output: Option[Account],
    ) extends Token

    final case class MintToken(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: DefinitionId,
        tokenId: TokenId,
        divisionIndex: Option[BigNat],
        outputs: Map[Account, BigNat],
    ) extends Token

    final case class TransferToken(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: DefinitionId,
        tokenId: TokenId,
        divisionIndex: Option[BigNat],
        inputTxs: Set[Signed.TxHash],
        outputs: Map[Account, BigNat],
    ) extends Token

    final case class CombineDivision(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: DefinitionId,
        tokenId: TokenId,
        inputTxs: Set[Input.Tx],
        amount: BigNat,
        divisionRemainder: Vector[BigNat],
    ) extends Token

    final case class DivideToken(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: DefinitionId,
        tokenId: TokenId,
        inputTx: Signed.TxHash,
        divisionAmount: BigNat,
        remainder: BigNat,
    ) extends Token

    type DefinitionId = UInt256Bytes @@ DefinitionIdTag
    type TokenId      = UInt256Bytes @@ TokenIdTag

    trait DefinitionIdTag
    trait TokenIdTag

    object DefinitionId {
      def apply(uint256bytes: UInt256Bytes): DefinitionId =
        shapeless.tag[DefinitionIdTag][UInt256Bytes](uint256bytes)
      implicit val eq: Eq[DefinitionId] = Eq.fromUniversalEquals
    }

    object TokenId {
      def apply(uint256bytes: UInt256Bytes): TokenId =
        shapeless.tag[TokenIdTag][UInt256Bytes](uint256bytes)
    }
  }

  implicit val circeDefIdDecoder: Decoder[DefinitionId] =
    taggedDecoder[UInt256Bytes, DefinitionIdTag]
  implicit val circeDefIdEncoder: Encoder[DefinitionId] =
    taggedEncoder[UInt256Bytes, DefinitionIdTag]

  implicit val circeTknIdDecoder: Decoder[TokenId] =
    taggedDecoder[UInt256Bytes, TokenIdTag]
  implicit val circeTknIdEncoder: Encoder[TokenId] =
    taggedEncoder[UInt256Bytes, TokenIdTag]

  implicit val byteEncoder: ByteEncoder[Transaction] = {
    case t: CreateName =>
      0.toByte +: ByteEncoder[CreateName].encode(t)
    case t: UpdateName =>
      1.toByte +: ByteEncoder[UpdateName].encode(t)
    case t: DeleteName =>
      2.toByte +: ByteEncoder[DeleteName].encode(t)
    case t: DefineToken =>
      3.toByte +: ByteEncoder[DefineToken].encode(t)
    case t: TransferAdmin =>
      4.toByte +: ByteEncoder[TransferAdmin].encode(t)
    case t: MintToken =>
      5.toByte +: ByteEncoder[MintToken].encode(t)
    case t: TransferToken =>
      6.toByte +: ByteEncoder[TransferToken].encode(t)
    case t: CombineDivision =>
      7.toByte +: ByteEncoder[CombineDivision].encode(t)
    case t: DivideToken =>
      8.toByte +: ByteEncoder[DivideToken].encode(t)
  }

  @SuppressWarnings(
    Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.Throw")
  )
  implicit val byteDecoder: ByteDecoder[Transaction] = {

    val (start, end) = (0, 8)

    val discriminator: ByteDecoder[Int] = {
      ByteDecoder.fromFixedSizeBytes(1)(_.head.toInt).emap {
        case n if start <= n && n <= end => Right(n)
        case n =>
          Left(
            DecodingFailure(s"Discriminator must be in [$start, $end], but: $n")
          )
      }
    }

    discriminator.flatMap {
      case 0 => ByteDecoder[CreateName].map(_.asInstanceOf[Transaction])
      case 1 => ByteDecoder[UpdateName].map(_.asInstanceOf[Transaction])
      case 2 => ByteDecoder[DeleteName].map(_.asInstanceOf[Transaction])
      case 3 => ByteDecoder[DefineToken].map(_.asInstanceOf[Transaction])
      case 4 => ByteDecoder[TransferAdmin].map(_.asInstanceOf[Transaction])
      case 5 => ByteDecoder[MintToken].map(_.asInstanceOf[Transaction])
      case 6 => ByteDecoder[TransferToken].map(_.asInstanceOf[Transaction])
      case 7 => ByteDecoder[CombineDivision].map(_.asInstanceOf[Transaction])
      case 8 => ByteDecoder[DivideToken].map(_.asInstanceOf[Transaction])
      case _ =>
        throw new RuntimeException(s"Transaction Decoder Implementation Error!")
    }
  }

  implicit val eqTx: Eq[Transaction] = Eq.fromUniversalEquals
  implicit val circeDecoder: Decoder[Transaction] = new Decoder[Transaction] {
    final def apply(c: HCursor): Decoder.Result[Transaction] =
      c.downField("type").as[String].flatMap {
        case "CreateName"      => c.downField("value").as[CreateName]
        case "UpdateName"      => c.downField("value").as[UpdateName]
        case "DeleteName"      => c.downField("value").as[DeleteName]
        case "DefineToken"     => c.downField("value").as[DefineToken]
        case "TransferAdmin"   => c.downField("value").as[TransferAdmin]
        case "MintToken"       => c.downField("value").as[MintToken]
        case "TransferToken"   => c.downField("value").as[TransferToken]
        case "CombineDivision" => c.downField("value").as[CombineDivision]
        case "DivideToken"     => c.downField("value").as[DivideToken]
        case t =>
          Left(
            io.circe.DecodingFailure(s"Wrong discriminator type: $t", c.history)
          )
      }
  }
  implicit val circeEncoder: Encoder[Transaction] = Encoder.instance {
    case t: CreateName =>
      Json.obj("type" -> "CreateName".asJson, "value" -> t.asJson)
    case t: UpdateName =>
      Json.obj("type" -> "UpdateName".asJson, "value" -> t.asJson)
    case t: DeleteName =>
      Json.obj("type" -> "DeleteName".asJson, "value" -> t.asJson)
    case t: DefineToken =>
      Json.obj("type" -> "DefineToken".asJson, "value" -> t.asJson)
    case t: TransferAdmin =>
      Json.obj("type" -> "TransferAdmin".asJson, "value" -> t.asJson)
    case t: MintToken =>
      Json.obj("type" -> "MintToken".asJson, "value" -> t.asJson)
    case t: TransferToken =>
      Json.obj("type" -> "TransferToken".asJson, "value" -> t.asJson)
    case t: CombineDivision =>
      Json.obj("type" -> "CombineDivision".asJson, "value" -> t.asJson)
    case t: DivideToken =>
      Json.obj("type" -> "DivideToken".asJson, "value" -> t.asJson)
  }
}
