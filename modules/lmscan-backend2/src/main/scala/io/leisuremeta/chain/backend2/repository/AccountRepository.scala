package io.leisuremeta.chain.lmscan
package backend2
import doobie.*
import doobie.implicits.*
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.common.model.DAO
import io.leisuremeta.chain.lmscan.backend2.CommonPipe.*

object AccountRepository:
  // import TxQueryPipe.*
  def getAccountAsync =
    sql"select * from account"
      .query[DAO.Account]
      .stream
      .take(1)
      .compile
      .toList

  def getAccount =
    sql"select * from account"
      .query[DAO.Account]
      .stream
      .take(1)
      .compile
      .toList
      .transact(xa)
      .attemptSql
