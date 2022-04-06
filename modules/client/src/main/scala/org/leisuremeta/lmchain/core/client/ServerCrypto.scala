package org.leisuremeta.lmchain.core
package client

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

import io.circe.generic.auto._
import io.circe.refined._
import io.circe.scalajs._
import org.scalajs.dom

import codec.circe._
import crypto.{CryptoOps, KeyPair}
import crypto.Hash.ops._
import model.{Account, Address, AccountSignature, Signed, Transaction}

@JSExportTopLevel("ServerCrypto")
object ServerCrypto {

  @JSExport
  def generatePrivateKey(): String = CryptoOps.generate().privateKey.toBytes.toHex

  @JSExport
  def addressFromPrivateKey(privateKey: String): String = {
    val keyPair: KeyPair = CryptoOps.fromPrivate(BigInt(privateKey, 16))
    Address.fromPublicKeyHash(keyPair.publicKey.toHash).bytes.toHex
  }

  @JSExport
  def sign(privateKey: String, tx: Transaction, name: js.UndefOr[String]): js.UndefOr[js.Any] = {
    signInner(privateKey, tx, name.toOption) match {
      case Left(msg) => dom.console.error(msg)
      case Right(signedTx) => signedTx.asJsAny
    }
  }

  private def signInner(privateKey: String, tx: Transaction, name: Option[String]): Either[String, Signed.Tx] = for {
    signature <- CryptoOps.sign(CryptoOps.fromPrivate(BigInt(privateKey, 16)), tx.toHash.toArray)
  } yield {
    val accountSignature = name match {
      case Some(value) =>
        AccountSignature.NamedSignature(
          Account.Name.from(value).toOption.get,
          signature,
        )
      case None => AccountSignature.UnnamedSignature(signature)
    }
    Signed(accountSignature, tx)
  }
}
