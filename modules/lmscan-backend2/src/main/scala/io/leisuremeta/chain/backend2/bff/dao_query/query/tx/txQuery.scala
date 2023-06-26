package io.leisuremeta.chain.lmscan
package backend2
import doobie.*
import doobie.implicits.*
import doobie.util.ExecutionContexts
import scala.reflect.runtime.universe.*

import scala.util.chaining.*
import cats.instances.boolean
import doobie.ConnectionIO
import scala.reflect.ClassTag

import fs2.Stream
import io.leisuremeta.chain.lmscan.common.model.DAO
import io.leisuremeta.chain.lmscan.backend2.CommonPipe.*

object TxQuery:
  import TxQueryPipe.*

  def getTxPipe(pipeString: Option[String]) =
    sql"select * from tx  ORDER BY  block_number DESC, event_time DESC  "
      .query[DAO.Tx] // DAO
      .stream
      .pipe(
        pipeString
          .pipe(genPipeList)
          .pipe(pipeRun),
      )
      .pipe(a => a)
      .compile
      .toList
      .transact(xa)
      .attemptSql

  def getTx =
    sql"select * from tx"
      .query[DAO.Tx] // DAO
      .stream
      .filter(t => t.blockNumber == 2.pipe(a => a))
      .pipe(a => a)
      .take(2)
      .compile // commont option
      .toList
      .transact(xa)
      .attemptSql

  def getTx_byAddress =
    sql"select * from tx"
      .query[DAO.Tx] // DAO
      .stream
      .filter(t => t.fromAddr == "playnomm" || t.toAddr.contains("playnomm"))
      .take(2)
      .compile // commont option
      .toList
      .transact(xa)
      .attemptSql

  def getAccount =
    sql"select * from account"
      .query[DAO.Account] // DAO
      .stream
      .take(1)
      .compile // commont option
      .toList
      .transact(xa)
      .attemptSql
