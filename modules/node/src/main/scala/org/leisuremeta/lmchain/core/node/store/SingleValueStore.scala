package org.leisuremeta.lmchain.core
package node.store

import cats.data.EitherT

import datatype.{UInt256Bytes, UInt256Refine}
import failure.DecodingFailure

trait SingleValueStore[F[_], A] {
  def get(): EitherT[F, DecodingFailure, Option[A]]
  def put(a: A): F[Unit]
}

object SingleValueStore {
  implicit def fromKeyValueStore[F[_], A](implicit
      kvStore: KeyValueStore[F, UInt256Bytes, A]
  ): SingleValueStore[F, A] = new SingleValueStore[F, A] {
    override def get(): EitherT[F, DecodingFailure, Option[A]] =
      kvStore.get(Key)
    override def put(a: A): F[Unit] = kvStore.put(Key, a)
  }

  private val Key: UInt256Bytes = UInt256Refine.EmptyBytes
}
