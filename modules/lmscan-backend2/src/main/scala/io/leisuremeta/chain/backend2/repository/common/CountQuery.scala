package io.leisuremeta.chain.lmscan
package backend2
import doobie.*
import doobie.implicits.*
import scala.util.chaining.*

object CountQuery:
  def getTxCount() =
    sql"SELECT COUNT(*) FROM Tx "
      .query[Int] // DAO
      .unique
      .transact(xa)
      .attemptSql
