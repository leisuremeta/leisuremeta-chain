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
    sql"select * from summary"
      .query[DAO.Summary]
      .unique
      .transact(xa)
      .attemptSql
