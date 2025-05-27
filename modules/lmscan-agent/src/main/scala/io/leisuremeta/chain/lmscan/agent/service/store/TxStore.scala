package io.leisuremeta.chain.lmscan.agent
package service

import doobie.util.transactor.Transactor
import doobie.*
import doobie.implicits.*
import cats.*
import cats.effect.*
import io.leisuremeta.chain.api.model.TransactionWithResult
import io.leisuremeta.chain.lmscan.agent.apps.Tx

case class TxRepository[F[_]: MonadCancelThrow](xa: Transactor[F]):
  def putTxState(
      hash: String,
      bHash: String,
      tx: TransactionWithResult,
      json: String,
  ) =
    sql"""insert into tx_state (hash, block_hash, json, event_time) values(
        $hash, $bHash, $json, ${tx.signedTx.value.createdAt.getEpochSecond})""".update.run
      .transact(xa)
      .attemptSql
  def putTx(
      tx: Tx,
  ) =
    sql"""insert into tx (hash, signer, token_type, tx_type, sub_type, block_hash, block_number, event_time) 
    values(${tx.hash}, ${tx.signer}, ${tx.tokenType}, ${tx.txType}, ${tx.subType}, ${tx.blockHash}, ${tx.blockNumber}, ${tx.eventTime})""".update.run
      .transact(xa)
      .attemptSql

object TxRepository:
  def build[F[_]: MonadCancelThrow](xa: Transactor[F]) = TxRepository(xa)
