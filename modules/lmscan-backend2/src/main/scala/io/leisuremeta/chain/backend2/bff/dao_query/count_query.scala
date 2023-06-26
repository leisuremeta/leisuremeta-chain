package io.leisuremeta.chain.lmscan
package backend2
import doobie.*
import doobie.implicits.*

import scala.util.chaining.*

import fs2.Stream

object CountQuery:
  def getTxCount() =
    sql"SELECT COUNT(*) FROM Tx "
      .query[Int] // DAO
      .unique
      .transact(xa)
      .attemptSql
