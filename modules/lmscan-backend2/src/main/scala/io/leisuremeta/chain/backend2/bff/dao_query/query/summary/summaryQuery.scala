package io.leisuremeta.chain.lmscan
package backend2
import doobie.*
import doobie.implicits.*
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.common.model.DAO
import io.leisuremeta.chain.lmscan.backend2.CommonPipe.*

object SummaryQuery:
//   import SummaryQueryPipe.*
  def getTx =
    sql"select * from tx"
      .query[DAO.Tx]
      .stream
      .filter(t => t.blockNumber == 2)
      .take(2)
      .compile
      .toList
      .transact(xa)
      .attemptSql

  def getSummary =
    sql"select * from summary"
      .query[DAO.Summary]
      .unique
      .transact(xa)
      .attemptSql
