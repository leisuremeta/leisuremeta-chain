package org.leisuremeta.lmchain.core
package crypto

import scala.scalajs.js.JSConverters._
import scala.scalajs.js.typedarray.Uint8Array

import eu.timepit.refined.refineV
import scodec.bits.ByteVector
import typings.bnJs.bnJsStrings.hex
import typings.elliptic.mod.{ec => EC}
import typings.elliptic.mod.ec.{KeyPair => JsKeyPair}
import typings.jsSha3.mod.{keccak256 => jsKeccak256}

import datatype.UInt256Refine

object CryptoOps {

  val ec: EC = new EC("secp256k1")

  def generate(): KeyPair = ec.genKeyPair().toScala

  def fromPrivate(privateKey: BigInt): KeyPair =
    ec.keyFromPrivate(privateKey.toByteArray.toUint8Array).toScala

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def keccak256(input: Array[Byte]): Array[Byte] = {

    val hexString: String = jsKeccak256.hex(input.toUint8Array)

    ByteVector
      .fromHexDescriptive(hexString)
      .fold(e => throw new Exception(e), _.toArray)
  }

  def sign(
      keyPair: KeyPair,
      transactionHash: Array[Byte],
  ): Either[String, Signature] = {
    val jsSig = keyPair.toJs.sign(transactionHash.toUint8Array)
    val recoveryParamEither = (jsSig.recoveryParam: Any) match {
      case d: Double => Right(d.toInt)
      case other     => Left(s"Expected double but received: $other")
    }
    for {
      recoveryParam <- recoveryParamEither
      v             <- refineV[Signature.HeaderRange](27 + recoveryParam)
      r             <- UInt256Refine.from(BigInt(jsSig.r.toString_hex(hex), 16))
      s             <- UInt256Refine.from(BigInt(jsSig.s.toString_hex(hex), 16))
    } yield Signature(v, r, s)
  }

  def recover(
      signature: Signature,
      hashArray: Array[Byte],
  ): Either[String, PublicKey] = ???

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  implicit class JsKeyPairOps(val jsKeyPair: JsKeyPair) extends AnyVal {
    def toScala: KeyPair = {
      val privHex = jsKeyPair.getPrivate().toString_hex(hex)
      val pubKey  = jsKeyPair.getPublic()
      val xHex    = pubKey.getX().toString_hex(hex)
      val yHex    = pubKey.getY().toString_hex(hex)
      val xBigInt = BigInt(xHex, 16)
      val yBigInt = BigInt(yHex, 16)
      val pBigInt = BigInt(privHex, 16)
      (for {
        p <- UInt256Refine.from(pBigInt)
        x <- UInt256Refine.from(xBigInt)
        y <- UInt256Refine.from(yBigInt)
      } yield KeyPair(p, PublicKey(x, y))).getOrElse(
        throw new Exception(s"Wrong keyPair: $privHex, $xHex, $yHex")
      )
    }
  }

  implicit class KeyPairOps(val keyPair: KeyPair) extends AnyVal {
    def toJs: JsKeyPair =
      ec.keyFromPrivate(keyPair.privateKey.toByteArray.toUint8Array)
  }

  @SuppressWarnings(
    Array("org.wartremover.warts.ArrayEquals")
  ) // May be wart remover bugs...
  implicit class ByteArrayOps(val byteArray: Array[Byte]) extends AnyVal {
    def toUint8Array: Uint8Array =
      Uint8Array.from[Byte](byteArray.toJSArray, _.toShort)
  }
}
