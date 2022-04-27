package org.leisuremeta.lmchain.core
package crypto

import model.{Block, Transaction}

trait Sign[A] {
  def apply(a: A, keyPair: KeyPair)(implicit
      hash: Hash[A]
  ): Either[String, Signature] = byHash(hash(a), keyPair)

  def byHash(
      hashValue: Hash.Value[A],
      keyPair: KeyPair,
  ): Either[String, Signature]
}

object Sign {
  def apply[A](implicit s: Sign[A]): Sign[A] = s

  object ops {
    implicit class SignOps(val keyPair: KeyPair) extends AnyVal {
      def sign[A: Hash: Sign](a: A): Either[String, Signature] =
        Sign[A].apply(a, keyPair)
    }

    implicit class HashValueSignOps[A](val hashValue: Hash.Value[A])
        extends AnyVal {
      def signBy(keyPair: KeyPair)(implicit
          sign: Sign[A]
      ): Either[String, Signature] =
        sign.byHash(hashValue, keyPair)
    }
  }

  implicit val signTransaction: Sign[Transaction] =
    (hashValue: Hash.Value[Transaction], keyPair: KeyPair) => {
      CryptoOps.sign(keyPair, hashValue.toArray)
    }

  implicit val signBlock: Sign[Block.Header] =
    (hashValue: Hash.Value[Block.Header], keyPair: KeyPair) => {
      CryptoOps.sign(keyPair, hashValue.toArray)
    }
}
