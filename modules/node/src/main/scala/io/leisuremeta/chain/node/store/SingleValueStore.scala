package io.leisuremeta.chain
package node.store

import cats.data.EitherT

import lib.datatype.{UInt256, UInt256Bytes}
import lib.failure.DecodingFailure

trait SingleValueStore[F[_], A]:
  def get(): EitherT[F, DecodingFailure, Option[A]]
  def put(a: A): F[Unit]

object SingleValueStore:
  def fromKeyValueStore[F[_], A](using
      kvStore: KeyValueStore[F, UInt256Bytes, A],
  ): SingleValueStore[F, A] = new SingleValueStore[F, A]:
    override def get(): EitherT[F, DecodingFailure, Option[A]] =
      kvStore.get(Key)
    override def put(a: A): F[Unit] = kvStore.put(Key, a)

  private val Key: UInt256Bytes = UInt256.EmptyBytes
