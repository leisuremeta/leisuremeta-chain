package io.leisuremeta.chain.lmscan.agent
package service

import doobie.util.transactor.Transactor
import doobie.*
import doobie.implicits.*
import cats.*
import cats.effect.*

case class SummaryRepository[F[_]: MonadCancelThrow](xa: Transactor[F]):
  def updateValidatorInfo =
    sql"""update validator_info set cnt = v.c from (select proposer p, count(1) c from block group by p) v
        where validator_info.address = v.p""".update.run
      .transact(xa)
      .attemptSql

  def updateSummary(
      balance: BigDecimal,
      cap: BigDecimal,
      supply: BigDecimal,
      price: BigDecimal,
  ) =
    sql"""
        INSERT INTO summary (lm_price, total_balance, market_cap, cir_supply, block_number, total_accounts, total_tx_size, total_nft)
        VALUES($price, $balance, $cap, $supply, 
          (SELECT number FROM block ORDER BY number DESC LIMIT 1),
          (SELECT count(1) FROM tx WHERE sub_type = 'CreateAccount' or sub_type = 'CreateAccountWithExternalChainAddresses'),
          (SELECT count(1) FROM tx),
          (SELECT count(1) FROM nft)
        )""".update.run
      .transact(xa)
      .attemptSql

object SummaryRepository:
  def build[F[_]: MonadCancelThrow](xa: Transactor[F]) =
    SummaryRepository(xa)
