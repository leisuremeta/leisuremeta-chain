package org.leisuremeta.lmchain.core

import java.time.Instant

import scala.collection.immutable.Queue

import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.util.Await
import io.circe.{Encoder, Json}
import io.circe.generic.auto._
import io.circe.refined._
import io.circe.parser.decode
import scodec.bits.ByteVector

import codec.circe._
import crypto._
import crypto.Hash.ops._
import datatype._
import model._

object console {

  val client: Service[Request, Response] = Http.client
    .withSessionQualifier.noFailureAccrual
    //.withTransport.tls("some.url")
    .newService(s"localhost:8081")

  val networkId: NetworkId = NetworkId(BigNat.unsafeFromBigInt(102))

  def name(s: String): Account.Name = {
    val Right(name) = Account.Name.from(s)
    name
  }

  def generate: KeyPair = CryptoOps.generate()

  def keyFromPrivate(privateKeyHex: String): KeyPair =
    CryptoOps.fromPrivate(BigInt(privateKeyHex, 16))

  case class Acc(
      name: Account.Name,
      keyPairs: Queue[KeyPair],
      guardian: Option[Account],
  ) {
    def create: Signed.Tx = {
      val tx: Transaction = Transaction.Name.CreateName(
        networkId = networkId,
        createdAt = Instant.now(),
        name = name,
        state = NameState(
          addressess = keyPairs
            .map(_.publicKey.toHash)
            .map(Address.fromPublicKeyHash(_))
            .map(_ -> BigNat.One)
            .toMap,
          threshold = BigNat.One,
          guardian = guardian,
        ),
      )
      sign(tx)
    }

    def update: Signed.Tx = {
      val tx: Transaction = Transaction.Name.UpdateName(
        networkId = networkId,
        createdAt = Instant.now(),
        name = name,
        state = NameState(
          addressess = keyPairs
            .map(_.publicKey.toHash)
            .map(Address.fromPublicKeyHash(_))
            .map(_ -> BigNat.One)
            .toMap,
          threshold = BigNat.One,
          guardian = guardian,
        ),
      )
      sign(tx)
    }

    def delete: Signed.Tx = {
      val tx: Transaction = Transaction.Name.DeleteName(
        networkId = networkId,
        createdAt = Instant.now(),
        name = name,
      )
      sign(tx)
    }

    def defineToken(tkndef: TknDef): Signed.Tx = {

      val tx: Transaction = Transaction.Token.DefineToken(
        networkId = networkId,
        createdAt = Instant.now(),
        name = tkndef.name,
        symbol = tkndef.symbol,
        definitionId = tkndef.definitionId,
        divisionSize = tkndef.divisionSize,
        data = ByteVector.empty,
      )
      sign(tx)
    }

    def mintToken(tkndef: TknDef, amount: BigInt, to: Account = this.account): Signed.Tx = {
      val tx: Transaction = Transaction.Token.MintToken(
        networkId = networkId,
        createdAt = Instant.now(),
        definitionId = tkndef.definitionId,
        tokenId = Transaction.Token.TokenId(tkndef.definitionId),
        divisionIndex = None,
        outputs = Map(to -> BigNat.unsafeFromBigInt(amount)),
      )
      sign(tx)
    }

    def transferToken(
        tkndef: TknDef,
        input: Signed.Tx,
        to: Map[Acc, Int],
    ): Signed.Tx = {
      val tx: Transaction = Transaction.Token.TransferToken(
        networkId = networkId,
        createdAt = Instant.now(),
        definitionId = tkndef.definitionId,
        tokenId = Transaction.Token.TokenId(tkndef.definitionId),
        divisionIndex = None,
        inputTxs = Set(input.toHash),
        outputs = to.map { case (k, v) =>
          Account.Named(k.name) -> BigNat.unsafeFromInt(v)
        }.toMap,
      )
      sign(tx)
    }

    def sign(tx: Transaction): Signed.Tx = {
      val Right(sig) = Sign[Transaction].apply(tx, keyPairs.head)
      val accountSig = AccountSignature.NamedSignature(name = name, sig = sig)
      Signed(accountSig, tx)
    }

    def getBalance: List[Transaction.Input.Tx] = {
      val request = Request(Method.Get, s"/balance/${name.toString}")
      val response = Await.result(client(request))
      decode[List[Transaction.Input.Tx]](response.contentString) match {
        case Left(error) => throw new RuntimeException(error.getMessage)
        case Right(txs) => txs
      }
    }

    def addKeyPair(keyPair: KeyPair): Acc =
      this.copy(keyPairs = this.keyPairs :+ keyPair)

    def removeKeyPair(keyPair: KeyPair): Acc =
      this.copy(keyPairs = this.keyPairs.filterNot(_ == keyPair))

    def setGuardian(account: Account): Acc = this.copy(guardian = Some(account))

    def account: Account = Account.Named(name)
  }
  object Acc {
    def apply(n: String): Acc =
      Acc(name = name(n), keyPairs = Queue.empty, guardian = None)
  }

  case class TknDef(
      name: Utf8,
      symbol: Utf8,
      divisionSize: BigNat,
      definitionId: Transaction.Token.DefinitionId,
  )

  val WONT_ID = UInt256Refine
    .from(
      ByteVector.fromValidHex(
        "9bde938a6e8026362367faa1dd97ad7c7e14fafe535d025ed7fbbc36f0df7829"
      )
    )
    .toOption
    .get

  val WONT: TknDef = TknDef(
    name = Utf8.unsafeFrom("WONT"),
    symbol = Utf8.unsafeFrom("WONT"),
    divisionSize = BigNat.Zero,
    definitionId = Transaction.Token.DefinitionId(WONT_ID),
  )

  private val txJsonEncoder = Encoder[Signed.Tx]

  implicit class TxOps(val tx: Signed.Tx) extends AnyVal {
    def toJson: Json = txJsonEncoder(tx)

    def submit: String = {
      val request = Request(Method.Post, "/transaction")
      request.setContentString(toJson.toString)
      request.setContentTypeJson()
      val response = Await.result(client(request))
      response.contentString
    }

    def txHash: String = {
      val request = Request(Method.Post, "/txhash")
      request.setContentString(toJson.toString)
      request.setContentTypeJson()
      val response = Await.result(client(request))
      response.contentString
    }
  }
}
