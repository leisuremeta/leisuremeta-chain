package org.leisuremeta.lmchain.core
package crypto

import model.{Block, Transaction}

trait Recover[A] {
  def apply(a: A, signature: Signature)(implicit
      hash: Hash[A]
  ): Option[PublicKey] = fromHash(hash(a), signature)

  def fromHash(
      hashValue: Hash.Value[A],
      signature: Signature,
  ): Option[PublicKey]
}

object Recover {
  def apply[A](implicit r: Recover[A]): Recover[A] = r

  object ops {
    implicit class RecoverOps[A](val a: A) extends AnyVal {
      def recover(signature: Signature)(implicit
          hash: Hash[A],
          recover: Recover[A],
      ): Option[PublicKey] = recover(a, signature)
    }

    implicit class HashValueRecoverOps[A](
        val hashValue: Hash.Value[A]
    ) extends AnyVal {
      def recover(signature: Signature)(implicit
          r: Recover[A]
      ): Option[PublicKey] =
        r.fromHash(hashValue, signature)
    }
  }

  implicit val recoverTx: Recover[Transaction] = {
    (hashValue: Transaction.TxHash, signature: Signature) =>
      CryptoOps.recover(signature, hashValue.toArray).toOption
  }

  implicit val recoverBlockHeader: Recover[Block.Header] = {
    (hashValue: Hash.Value[Block.Header], signature: Signature) =>
      CryptoOps.recover(signature, hashValue.toArray).toOption
  }
}
