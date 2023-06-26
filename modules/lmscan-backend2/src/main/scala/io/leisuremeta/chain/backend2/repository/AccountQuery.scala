package io.leisuremeta.chain.lmscan
package backend2
import doobie.*
import doobie.implicits.*
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.common.model.DAO
import io.leisuremeta.chain.lmscan.backend2.CommonPipe.*

object AccountQuery:
  // import TxQueryPipe.*

  def getAccount =
    sql"select * from account"
      .query[DAO.Account]
      .stream
      .take(1)
      .compile
      .toList
      .transact(xa)
      .attemptSql

  // def _getAccount =
  //   sql"select * from account"
  //     .query[DAO.Account]

  //     .toList
  //     .transact(xa)
  //     .attemptSql
