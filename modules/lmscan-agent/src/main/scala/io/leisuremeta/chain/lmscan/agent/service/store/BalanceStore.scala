package io.leisuremeta.chain.lmscan.agent
package service

import doobie.util.transactor.Transactor
import doobie.*
import doobie.implicits.*
import cats.*
import cats.effect.*
import io.leisuremeta.chain.lmscan.agent.apps.*
import cats.data.NonEmptyList

given inputWrite: Write[LedgerType.InputLedger] =
  Write[(String, String, String)].contramap: v =>
    (v.hash, v.address, v.free.toString)
given ledgerWrite: Write[LedgerType.SpendLedger] =
  Write[(String, String, String)].contramap: v =>
    (v.hash, v.address, v.used)
given lockWrite: Write[LedgerType.LockLedger] =
  Write[(String, String, String)].contramap: v =>
    (v.hash, v.address, v.locked.toString)
given returnWrite: Write[LedgerType.SpendLockLedger] =
  Write[(String, String)].contramap: v =>
    (v.hash, v.used)

case class BalanceRepository[F[_]: MonadCancelThrow](xa: Transactor[F]):
  def updateBalance(txs: List[(String, BigDecimal)]) =
    Update[(String, BigDecimal)](
      "insert into balance (address ,free, updated_at) values(?, ?, EXTRACT(epoch FROM now())) on conflict (address) do update set free = excluded.free, updated_at = EXTRACT(epoch FROM now())",
    ).updateMany(txs).transact(xa).attemptSql

case class LocalBalanceRepository[F[_]: MonadCancelThrow](xa: Transactor[F]):
  def createLedgerTable =
    sql"""create table if not exists ledger (
          hash text,
          address text,
          free text,
          used text,
          primary key (hash, address)
        );""".update.run.transact(xa)

  def createLockLedgerTable =
    sql"""create table if not exists lock_ledger (
          hash text,
          address text,
          locked text,
          used text,
          primary key (hash)
        );""".update.run.transact(xa)

  def addInputLedger(txs: List[LedgerType.InputLedger]) =
    Update[LedgerType.InputLedger](
      "insert into ledger (hash, address, free) values(?, ?, ?) on conflict (hash, address) do update set free = excluded.free",
    ).updateMany(txs).transact(xa).attemptSql

  def addSpendLedger(txs: List[LedgerType.SpendLedger]) =
    Update[LedgerType.SpendLedger](
      "insert into ledger (hash, address, used) values(?, ?, ?) on conflict (hash, address) do update set used = excluded.used",
    ).updateMany(txs).transact(xa).attemptSql

  def addLockLedger(txs: List[LedgerType.LockLedger]) =
    Update[LedgerType.LockLedger](
      "insert into lock_ledger (hash, address, locked) values(?, ?, ?) on conflict (hash) do update set address = excluded.address, locked = excluded.locked",
    ).updateMany(txs).transact(xa).attemptSql

  def addSpendLockLedger(txs: List[LedgerType.SpendLockLedger]) =
    Update[LedgerType.SpendLockLedger](
      "insert into lock_ledger (hash, used) values(?, ?) on conflict (hash) do update set used = excluded.used",
    ).updateMany(txs).transact(xa).attemptSql

  def getLedger(
      qs: NonEmptyList[String],
  ) =
    val q =
      fr"""select address, free from ledger where used is null and free is not null and """
        ++ Fragments
          .in(fr"address", qs)

    q.query[(String, String)]
      .to[List]
      .transact(xa)
      .attemptSql

object BalanceRepository:
  def build[F[_]: MonadCancelThrow](xa: Transactor[F]) =
    BalanceRepository(xa)

object LocalBalanceRepository:
  def build[F[_]: MonadCancelThrow](xa: Transactor[F]) =
    LocalBalanceRepository(xa)
