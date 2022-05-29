package io.leisuremeta.chain
package node.store

import cats.data.EitherT
import lib.failure.DecodingFailure

trait StoreIndex[F[_], K, V] extends KeyValueStore[F, K, V] {
  def from(
      key: K,
      offset: Int,
      limit: Int,
  ): EitherT[F, DecodingFailure, List[(K, V)]]
}
