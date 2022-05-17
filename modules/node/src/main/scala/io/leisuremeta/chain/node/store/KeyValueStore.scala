package io.leisuremeta.chain
package node.store

import cats.data.EitherT
import lib.failure.DecodingFailure

trait KeyValueStore[F[_], K, V]:
  def get(key: K): EitherT[F, DecodingFailure, Option[V]]
  def put(key: K, value: V): F[Unit]
  def remove(key: K): F[Unit]
