package io.leisuremeta.chain.lib.crypto

trait Sign[A]:
  def apply(a: A, keyPair: KeyPair)(implicit
      hash: Hash[A],
  ): Either[String, Signature] = byHash(hash(a), keyPair)

  def byHash(
      hashValue: Hash.Value[A],
      keyPair: KeyPair,
  ): Either[String, Signature]

object Sign:
  def apply[A: Sign]: Sign[A] = summon

  object ops:
    extension (keyPair: KeyPair)
      def sign[A: Hash: Sign](a: A): Either[String, Signature] =
        Sign[A].apply(a, keyPair)

    extension [A](hashValue: Hash.Value[A])
      def signBy(keyPair: KeyPair)(using
          sign: Sign[A],
      ): Either[String, Signature] = sign.byHash(hashValue, keyPair)
