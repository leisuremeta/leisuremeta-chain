package org.leisuremeta.lmchain.core
package client

import java.time.Instant

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

import cats.implicits._

import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import io.circe.Decoder
import io.circe.syntax._
import scodec.bits.ByteVector
import shapeless.tag

import crypto.Hash
import datatype.{BigNat, UInt256Bytes, UInt256Refine, Utf8}
import model._

object Tx {

  @JSExportTopLevel("TxName")
  object TxName {

    @JSExport
    def nameState(
        addressess: js.Dictionary[Int],
        threshold: Int,
        guardian: UndefOr[String],
    ): NameState = {
      val addressMap: Map[Address, BigNat] = addressess.toList
        .traverse { case (addressHex, weight) =>
          for {
            address <- Address.fromHex(addressHex).leftMap(_.msg)
            w       <- refineV[NonNegative](BigInt(weight))
          } yield (address, w)
        }
        .map[Map[Address, BigNat]](_.toMap)
        .left
        .map(msg => throw new Exception(msg))
        .merge
      val g: Option[Account] = for {
        s <- guardian.toOption
        g <- Decoder[Account].decodeJson(s.asJson).toOption
      } yield g

      NameState(
        addressess = addressMap,
        threshold = BigNat.unsafeFromInt(threshold),
        guardian = g,
      )
    }

    @JSExport
    def create(
        networkId: Int,
        name: String,
        nameState: NameState,
        createdAt: Instant = Instant.now(),
    ): Transaction.Name.CreateName = {
      val Right(name1) = Account.Name.from(name)

      Transaction.Name.CreateName(
        networkId = NetworkId(BigNat.unsafeFromBigInt(BigInt(networkId))),
        createdAt = createdAt,
        name = name1,
        state = nameState,
      )
    }

    @JSExport
    def update(
        networkId: Int,
        name: String,
        nameState: NameState,
        createdAt: Instant = Instant.now(),
    ): Transaction.Name.UpdateName = {
      val Right(name1) = Account.Name.from(name)

      Transaction.Name.UpdateName(
        networkId = NetworkId(BigNat.unsafeFromBigInt(BigInt(networkId))),
        createdAt = createdAt,
        name = name1,
        state = nameState,
      )
    }

    @JSExport
    def delete(
        networkId: Int,
        name: String,
        createdAt: Instant = Instant.now(),
    ): Transaction.Name.DeleteName = {
      val Right(name1) = Account.Name.from(name)

      Transaction.Name.DeleteName(
        networkId = NetworkId(BigNat.unsafeFromBigInt(BigInt(networkId))),
        createdAt = createdAt,
        name = name1,
      )
    }
  }

  @JSExportTopLevel("TxToken")
  object TxToken {

    @JSExport
    def uint256(hex: String): UInt256Bytes = (for {
      bytes <- ByteVector.fromHexDescriptive(hex)
      uint  <- UInt256Refine.from(bytes)
    } yield uint).left.map(msg => throw new Exception(msg)).merge

    @JSExport
    def defineToken(
        networkId: Int,
        definitionId: UInt256Bytes,
        name: String,
        symbol: String,
        divisionSize: Int,
        data: String,
        createdAt: Instant = Instant.now(),
    ): Transaction.Token.DefineToken = {
      Transaction.Token.DefineToken(
        networkId = NetworkId(BigNat.unsafeFromBigInt(BigInt(networkId))),
        createdAt = createdAt,
        definitionId = Transaction.Token.DefinitionId(definitionId),
        name = Utf8.unsafeFrom(name),
        symbol = Utf8.unsafeFrom(symbol),
        divisionSize = BigNat.unsafeFromInt(divisionSize),
        data = ByteVector.fromBase64(data).getOrElse(ByteVector.empty),
      )
    }

    @JSExport
    def transferAdmin(
        networkId: Int,
        definitionId: UInt256Bytes,
        output: UndefOr[Account],
        createdAt: Instant = Instant.now(),
    ): Transaction.Token.TransferAdmin = {
      Transaction.Token.TransferAdmin(
        networkId = NetworkId(BigNat.unsafeFromBigInt(BigInt(networkId))),
        createdAt = createdAt,
        definitionId = Transaction.Token.DefinitionId(definitionId),
        output = output.toOption,
      )
    }

    @JSExport
    def mintToken(
        networkId: Int,
        definitionId: UInt256Bytes,
        tokenId: UInt256Bytes,
        divisionIndex: UndefOr[Int],
        outputs: js.Dictionary[Int],
        createdAt: Instant = Instant.now(),
    ): Transaction.Token.MintToken = {
      val outputMap: Map[Account, BigNat] = outputs.toList
        .traverse { case (accountString, weight) =>
          for {
            account <- Account.circeAccountKeyDecoder.apply(accountString)
            w       <- refineV[NonNegative](BigInt(weight)).toOption
          } yield (account, w)
        }
        .map[Map[Account, BigNat]](_.toMap)
        .get

      Transaction.Token.MintToken(
        networkId = NetworkId(BigNat.unsafeFromBigInt(BigInt(networkId))),
        createdAt = createdAt,
        definitionId = Transaction.Token.DefinitionId(definitionId),
        tokenId = Transaction.Token.TokenId(tokenId),
        divisionIndex = divisionIndex.toOption.map(BigNat.unsafeFromInt),
        outputs = outputMap,
      )
    }

    @JSExport
    def txInput(
        hash: String,
        divisionIndex: UndefOr[Int],
    ): Transaction.Input.Tx = {
      Transaction.Input.Tx(
        refineTxHash(hash),
        divisionIndex.toOption.map(BigNat.unsafeFromInt),
      )
    }

    private def refineTxHash(
        hashString: String
    ): Hash.Value[Signed[Transaction]] = (for {
      bytes   <- ByteVector.fromHex(hashString)
      refined <- UInt256Refine.from(bytes).toOption
    } yield tag[Signed[Transaction]][UInt256Bytes](refined)).get

    @JSExport
    def transferToken(
        networkId: Int,
        definitionId: UInt256Bytes,
        tokenId: UInt256Bytes,
        divisionIndex: UndefOr[Int],
        inputTxs: js.Array[String],
        outputs: js.Dictionary[Int],
        createdAt: Instant = Instant.now(),
    ): Transaction.Token.TransferToken = {
      val outputMap: Map[Account, BigNat] = outputs.toList
        .traverse { case (accountString, weight) =>
          for {
            account <- Account.circeAccountKeyDecoder.apply(accountString)
            w       <- refineV[NonNegative](BigInt(weight)).toOption
          } yield (account, w)
        }
        .map[Map[Account, BigNat]](_.toMap)
        .get

      Transaction.Token.TransferToken(
        networkId = NetworkId(BigNat.unsafeFromBigInt(BigInt(networkId))),
        createdAt = createdAt,
        definitionId = Transaction.Token.DefinitionId(definitionId),
        tokenId = Transaction.Token.TokenId(tokenId),
        divisionIndex = divisionIndex.toOption.map(BigNat.unsafeFromInt),
        inputTxs = inputTxs.toArray.map(refineTxHash).toSet,
        outputs = outputMap,
      )
    }

    @JSExport
    def combineDivision(
        networkId: Int,
        definitionId: UInt256Bytes,
        tokenId: UInt256Bytes,
        inputTxs: js.Array[Transaction.Input.Tx],
        amount: String,
        divisionRemainder: js.Array[Int],
        createdAt: Instant = Instant.now(),
    ): Transaction.Token.CombineDivision = {

      Transaction.Token.CombineDivision(
        networkId = NetworkId(BigNat.unsafeFromBigInt(BigInt(networkId))),
        createdAt = createdAt,
        definitionId = Transaction.Token.DefinitionId(definitionId),
        tokenId = Transaction.Token.TokenId(tokenId),
        inputTxs = inputTxs.toArray.toSet,
        amount = BigNat.unsafeFromBigInt(BigInt(amount)),
        divisionRemainder =
          divisionRemainder.toArray.map(BigNat.unsafeFromInt).toVector,
      )
    }

    @JSExport
    def divideToken(
        networkId: Int,
        definitionId: UInt256Bytes,
        tokenId: UInt256Bytes,
        inputTx: String,
        divisionAmount: String,
        remainder: String,
        createdAt: Instant = Instant.now(),
    ): Transaction.Token.DivideToken = {

      Transaction.Token.DivideToken(
        networkId = NetworkId(BigNat.unsafeFromBigInt(BigInt(networkId))),
        createdAt = createdAt,
        definitionId = Transaction.Token.DefinitionId(definitionId),
        tokenId = Transaction.Token.TokenId(tokenId),
        inputTx = refineTxHash(inputTx),
        divisionAmount = BigNat.unsafeFromBigInt(BigInt(divisionAmount)),
        remainder = BigNat.unsafeFromBigInt(BigInt(remainder)),
      )
    }

  }
}
