package org.leisuremeta.lmchain.core
package node.store

import cats.data.EitherT

import crypto.Hash
import crypto.Hash.ops._
import failure.DecodingFailure

trait HashStore[F[_], A] {
  def get(hash: Hash.Value[A]): EitherT[F, DecodingFailure, Option[A]]
  def put(a: A): F[Unit]
  def remove(hash: Hash.Value[A]): F[Unit]
}

object HashStore {
  implicit def fromKeyValueStore[F[_], A: Hash](implicit
      kvStore: KeyValueStore[F, Hash.Value[A], A]
  ): HashStore[F, A] = new HashStore[F, A] {
    override def get(
        hash: Hash.Value[A]
    ): EitherT[F, DecodingFailure, Option[A]]         = kvStore.get(hash)
    override def put(a: A): F[Unit]                   = kvStore.put(a.toHash, a)
    override def remove(hash: Hash.Value[A]): F[Unit] = kvStore.remove(hash)
  }
}
