package io.leisuremeta.chain.lib
package crypto

trait Recover[A]:
  def apply(a: A, signature: Signature)(implicit
      hash: Hash[A],
  ): Option[PublicKey] = fromHash(hash(a), signature)

  def fromHash(
      hashValue: Hash.Value[A],
      signature: Signature,
  ): Option[PublicKey]

object Recover:
  def apply[A: Recover]: Recover[A] = summon

  object ops:
    extension [A](hashValue: Hash.Value[A])
      def recover(signature: Signature)(using
          r: Recover[A],
      ): Option[PublicKey] = r.fromHash(hashValue, signature)
