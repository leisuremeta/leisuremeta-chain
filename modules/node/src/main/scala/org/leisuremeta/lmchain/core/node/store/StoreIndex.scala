package org.leisuremeta.lmchain.core
package node.store

import cats.data.EitherT
import failure.DecodingFailure

trait StoreIndex[F[_], K, V] extends KeyValueStore[F, K, V] {
  def from(
      key: K,
      offset: Int,
      limit: Int,
  ): EitherT[F, DecodingFailure, List[(K, V)]]
}
