package org.leisuremeta.lmchain.core
package crypto

import java.security.{KeyPairGenerator, SecureRandom, Security}
import java.security.spec.ECGenParameterSpec
import java.math.BigInteger
import java.util.Arrays

import cats.derived.auto.eq._
import cats.implicits._

import eu.timepit.refined.refineV
import org.bouncycastle.asn1.x9.{X9ECParameters, X9IntegerConverter}
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.{
  ECDomainParameters,
  ECPrivateKeyParameters,
}
import org.bouncycastle.crypto.signers.{ECDSASigner, HMacDSAKCalculator}
import org.bouncycastle.jcajce.provider.asymmetric.ec.{
  BCECPrivateKey,
  BCECPublicKey,
}
import org.bouncycastle.jcajce.provider.digest.Keccak
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve
import org.bouncycastle.math.ec.{
  ECAlgorithms,
  ECPoint,
  FixedPointCombMultiplier,
}
import shapeless.syntax.typeable._

import datatype.{UInt256BigInt, UInt256Refine}

object CryptoOps {

  locally {
    @SuppressWarnings(
      Array(
        "org.wartremover.warts.Equals",
        "org.wartremover.warts.NonUnitStatements",
      )
    )
    val _ =
      if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
        Security.addProvider(new BouncyCastleProvider())
      }
  }

  val secureRandom: SecureRandom = new SecureRandom()

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def generate(): KeyPair = {
    val gen  = KeyPairGenerator.getInstance("ECDSA", "BC")
    val spec = new ECGenParameterSpec("secp256k1")
    gen.initialize(spec, secureRandom)
    val pair = gen.generateKeyPair
    (for {
      bcecPrivate <- pair.getPrivate.cast[BCECPrivateKey]
      bcecPublic  <- pair.getPublic.cast[BCECPublicKey]
      privateKey  <- UInt256Refine.from(BigInt(bcecPrivate.getD)).toOption
      publicKey <- PublicKey
        .fromByteArray(bcecPublic.getQ.getEncoded(false).tail)
        .toOption
    } yield KeyPair(privateKey, publicKey)).getOrElse {
      throw new Exception(s"Wrong keypair result: $pair")
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def fromPrivate(privateKey: BigInt): KeyPair = {
    val point: ECPoint = new FixedPointCombMultiplier()
      .multiply(Curve.getG, privateKey.bigInteger mod Curve.getN)
    val encoded: Array[Byte] = point.getEncoded(false)
    (for {
      private256 <- UInt256Refine.from(privateKey)
      public <- PublicKey.fromByteArray(
        Arrays.copyOfRange(encoded, 1, encoded.length)
      )
    } yield KeyPair(private256, public)) match {
      case Right(keypair) => keypair
      case Left(msg)      => throw new Exception(msg)
    }
  }

  implicit val arrayEq: cats.Eq[Array[Byte]] = cats.Eq.fromUniversalEquals

  def keccak256(input: Array[Byte]): Array[Byte] = {
    val kecc = new Keccak.Digest256()
    kecc.update(input, 0, input.length)
    kecc.digest()
  }

  def sign(
      keyPair: KeyPair,
      transactionHash: Array[Byte],
  ): Either[String, Signature] = {
    val signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()))
    signer.init(
      true,
      new ECPrivateKeyParameters(keyPair.privateKey.bigInteger, Curve),
    )
    val Array(r, sValue) = signer.generateSignature(transactionHash)
    val s =
      if (BigInt(sValue) > HalfCurveOrder) Curve.getN subtract sValue
      else sValue
    for {
      r256 <- UInt256Refine.from(BigInt(r))
      s256 <- UInt256Refine.from(BigInt(s))
      recId <- (0 until 4)
        .find { id =>
          recoverFromSignature(id, r256, s256, transactionHash) === Some(
            keyPair.publicKey
          )
        }
        .toRight {
          "Could not construct a recoverable key. The credentials might not be valid."
        }
      v <- refineV[Signature.HeaderRange](recId + 27)
    } yield Signature(v, r256, s256)
  }

  def recover(
      signature: Signature,
      hashArray: Array[Byte],
  ): Either[String, PublicKey] = {
    val header = signature.v.value & 0xff
    val recId  = header - 27
    recoverFromSignature(recId, signature.r, signature.s, hashArray)
      .toRight("Could not recover public key from signature")
  }

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  private[crypto] def recoverFromSignature(
      recId: Int,
      r: UInt256BigInt,
      s: UInt256BigInt,
      message: Array[Byte],
  ): Option[PublicKey] = {
    require(recId >= 0, "recId must be positive")
    require(message =!= null, "message cannot be null")

    val n     = Curve.getN
    val x     = r.bigInteger add (n multiply BigInteger.valueOf(recId.toLong / 2))
    val prime = SecP256K1Curve.q
    if (x.compareTo(prime) >= 0) None
    else {
      val R = {
        def decompressKey(xBN: BigInteger, yBit: Boolean): ECPoint = {
          val x9 = new X9IntegerConverter()
          val compEnc: Array[Byte] =
            x9.integerToBytes(xBN, 1 + x9.getByteLength(Curve.getCurve()))
          compEnc(0) = if (yBit) 0x03 else 0x02
          Curve.getCurve().decodePoint(compEnc)
        }
        decompressKey(x, (recId & 1) === 1)
      }
      if (!R.multiply(n).isInfinity()) None
      else {
        val e        = new BigInteger(1, message)
        val eInv     = BigInteger.ZERO subtract e mod n
        val rInv     = r.bigInteger modInverse n
        val srInv    = rInv multiply s.bigInteger mod n
        val eInvrInv = rInv multiply eInv mod n
        val q: ECPoint =
          ECAlgorithms.sumOfTwoMultiplies(Curve.getG(), eInvrInv, R, srInv)
        PublicKey.fromByteArray(q.getEncoded(false).tail).toOption
      }
    }

  }

  val CurveParams: X9ECParameters = CustomNamedCurves.getByName("secp256k1")
  val Curve: ECDomainParameters = new ECDomainParameters(
    CurveParams.getCurve,
    CurveParams.getG,
    CurveParams.getN,
    CurveParams.getH,
  )
  val HalfCurveOrder: BigInt = BigInt(CurveParams.getN) / 2
}
