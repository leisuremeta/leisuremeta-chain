package io.leisuremeta.chain.lmscan.agent
package service

import doobie.util.transactor.Transactor
import doobie.*
import doobie.implicits.*
import cats.*
import cats.effect.*
import io.leisuremeta.chain.lmscan.backend.entity.Block

case class BlockRepository[F[_]: MonadCancelThrow](xa: Transactor[F]):
  def getLatestBlock =
    sql"select number, hash, parent_hash, tx_count, event_time, created_at, proposer from block order by number desc limit 1"
      .query[Block]
      .option
      .transact(xa)
      .attemptSql

  def getLowestBlock =
    sql"select number, hash, parent_hash, tx_count, event_time, created_at, proposer from block order by number limit 1"
      .query[Block]
      .option
      .transact(xa)
      .attemptSql

  def putBlock(hash: String, num: Long, pHash: String, txC: Int, et: Long) =
    sql"""insert into block (hash, number, parent_hash, tx_count, event_time, proposer) values(
        $hash, $num, $pHash, $txC, $et, (select address from validator_info where ${et % 4} = id))""".update.run
      .transact(xa)

object BlockRepository:
  def build[F[_]: MonadCancelThrow](xa: Transactor[F]) =
    BlockRepository(xa)
