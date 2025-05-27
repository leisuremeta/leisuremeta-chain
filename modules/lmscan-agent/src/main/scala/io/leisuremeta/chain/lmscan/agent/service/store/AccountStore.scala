package io.leisuremeta.chain.lmscan.agent
package service

import doobie.util.transactor.Transactor
import doobie.*
import doobie.implicits.*
import cats.*
import cats.effect.*
import io.leisuremeta.chain.api.model.Account

case class AccountRepository[F[_]: MonadCancelThrow](xa: Transactor[F]):
  def putAccountMapper(
      hash: String,
      accounts: Set[Account],
  ) =
    Update[(String, String)](s"""insert into account_mapper (hash, address) 
    values(?, ?) on conflict (hash, address) do nothing""")
      .updateMany(accounts.toList.map(a => (hash, a.toString)))
      .transact(xa)
      .attemptSql

object AccountRepository:
  def build[F[_]: MonadCancelThrow](xa: Transactor[F]) = AccountRepository(xa)
