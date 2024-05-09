package io.leisuremeta.chain.lib
package crypto

import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.typedarray.Uint8Array

import io.github.iltotore.iron.*
import scodec.bits.ByteVector
import typings.bnJs.bnJsStrings.hex
import typings.elliptic.mod.{ec as EC}
import typings.elliptic.mod.ec.{KeyPair as JsKeyPair}
import typings.jsSha3.mod.{keccak256 as jsKeccak256}

import datatype.UInt256

object CryptoOps:

  val ec: EC = new EC("secp256k1")

  def generate(): KeyPair = ec.genKeyPair().toScala

  def fromPrivate(privateKey: BigInt): KeyPair =
    ec.keyFromPrivate(privateKey.toByteArray.toUint8Array).toScala

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def keccak256(input: Array[Byte]): Array[Byte] =

    val hexString: String = jsKeccak256.hex(input.toUint8Array)

    ByteVector
      .fromHexDescriptive(hexString)
      .fold(e => throw new Exception(e), _.toArray)

  def sign(
      keyPair: KeyPair,
      transactionHash: Array[Byte],
  ): Either[String, Signature] =
    val jsSig = keyPair.toJs.sign(transactionHash.toUint8Array)
    val recoveryParamEither:Either[String, Int] = (jsSig.recoveryParam: Any) match
      case d: Double => Right[String, Int](d.toInt)
      case other     => Left[String, Int](s"Expected double but received: $other")
    for
      recoveryParam <- recoveryParamEither
      v             <- (27 + recoveryParam).refineEither[Signature.HeaderRange]
      r <- UInt256.from(BigInt(jsSig.r.toString_hex(hex), 16)).left.map(_.msg)
      s <- UInt256.from(BigInt(jsSig.s.toString_hex(hex), 16)).left.map(_.msg)
    yield Signature(v, r, s)

  @SuppressWarnings(Array("org.wartremover.warts.TripleQuestionMark"))
  def recover(
      signature: Signature,
      hashArray: Array[Byte],
  ): Either[String, PublicKey] = ???

  extension(jsKeyPair: JsKeyPair)
    @SuppressWarnings(Array("org.wartremover.warts.Throw"))
    def toScala: KeyPair =
      val privHex = jsKeyPair.getPrivate().toString_hex(hex)
      val pubKey  = jsKeyPair.getPublic()
      val xHex    = pubKey.getX().toString_hex(hex)
      val yHex    = pubKey.getY().toString_hex(hex)
      val xBigInt = BigInt(xHex, 16)
      val yBigInt = BigInt(yHex, 16)
      val pBigInt = BigInt(privHex, 16)
      (for
        p <- UInt256.from(pBigInt)
        x <- UInt256.from(xBigInt)
        y <- UInt256.from(yBigInt)
      yield KeyPair(p, PublicKey(x, y))).getOrElse(
        throw new Exception(s"Wrong keyPair: $privHex, $xHex, $yHex"),
      )

  extension(keyPair: KeyPair)
    def toJs: JsKeyPair =
      ec.keyFromPrivate(keyPair.privateKey.toByteArray.toUint8Array)

  extension(byteArray: Array[Byte])
    def toUint8Array: Uint8Array =
      Uint8Array.from[Byte](byteArray.toJSArray, _.toShort)
