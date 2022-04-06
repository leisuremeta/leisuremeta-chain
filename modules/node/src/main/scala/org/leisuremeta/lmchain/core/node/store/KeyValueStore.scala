package org.leisuremeta.lmchain.core
package node.store

import cats.data.EitherT
import failure.DecodingFailure

trait KeyValueStore[F[_], K, V] {
  def get(key: K): EitherT[F, DecodingFailure, Option[V]]
  def put(key: K, value: V): F[Unit]
  def remove(key: K): F[Unit]
}
