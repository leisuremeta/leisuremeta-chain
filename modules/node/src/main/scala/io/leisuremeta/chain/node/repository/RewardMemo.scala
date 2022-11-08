package io.leisuremeta.chain
package node
package repository

import java.time.Instant

import cats.Functor
import cats.data.EitherT
import cats.syntax.either.*

import api.model.Account
import api.model.reward.DaoActivity
import api.model.token.TokenId
import store.KeyValueStore

opaque type RewardMemo[F[_], K] = KeyValueStore[F, K, DaoActivity]

extension [F[_]: Functor, K](m: RewardMemo[F, K])
  def get(key: K): EitherT[F, String, Option[DaoActivity]] = m.get(key).leftMap(_.msg)
  def put(key: K, activity: DaoActivity): F[Unit] = m.put(key, activity)

object RewardMemo:

  def apply[F[_], K](
      kvStore: KeyValueStore[F, K, DaoActivity],
  ): RewardMemo[F, K] = kvStore

  type UserActed[F[_]] = RewardMemo[F, (Instant, Account)]
  object UserActed:
    given fromKvStore[F[_]](using
        kvStore: KeyValueStore[F, (Instant, Account), DaoActivity],
    ): UserActed[F] = apply(kvStore)

  type TokenReceived[F[_]] = RewardMemo[F, (Instant, TokenId)]
  object TokenReceived:
    given fromKvStore[F[_]](using
        kvStore: KeyValueStore[F, (Instant, TokenId), DaoActivity],
    ): TokenReceived[F] = apply(kvStore)

  type TotalActivity[F[_]] = RewardMemo[F, Instant]
  object TotalActivity:
    given fromKvStore[F[_]](using
        kvStore: KeyValueStore[F, Instant, DaoActivity],
    ): TotalActivity[F] = apply(kvStore)
