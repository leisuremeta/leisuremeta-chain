package io.leisuremeta.chain.lmscan
package backend2
import doobie.*
import doobie.implicits.*
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.common.model.DAO
import io.leisuremeta.chain.lmscan.backend2.CommonPipe.*

object SummaryQuery:
//   import SummaryQueryPipe.*

  def getSummary =
    sql"select * from summary ORDER BY created_at DESC"
      .query[DAO.Summary]
      .stream
      .take(1)
      .compile
      .toList
      .transact(xa)
      .attemptSql
