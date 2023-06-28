package io.leisuremeta.chain.lmscan
package backend2
import doobie.*
import doobie.implicits.*
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.common.model.DAO
import io.leisuremeta.chain.lmscan.backend2.CommonPipe.*

object TxRepository:
  import TxRepositoryPipe.*
  def getTxPipeAsync(pipeString: Option[String]) =
    sql"select * from tx  ORDER BY  block_number DESC, event_time DESC  "
      .query[DAO.Tx]
      .stream
      .pipe(
        pipeString
          .pipe(genPipeList)
          .pipe(pipeRun),
      )
      .take(100)
      .compile
      .toList

  def getTxPipe(pipeString: Option[String]) =
    sql"select * from tx  ORDER BY  block_number DESC, event_time DESC  "
      .query[DAO.Tx]
      .stream
      .pipe(
        pipeString
          .pipe(genPipeList)
          .pipe(pipeRun),
      )
      .compile
      .toList
      .transact(xa)
      .attemptSql

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

  def getTx2 =
    sql"select * from tx"
      .query[DAO.Tx]
      .stream
      .take(1)
      .compile
      .toList

  // def getTx_byAddress =
  //   sql"select * from tx"
  //     .query[DAO.Tx]
  //     .stream
  //     .filter(t => t.fromAddr == "playnomm" || t.toAddr.contains("playnomm"))
  //     .take(2)
  //     .compile
  //     .toList
  //     .transact(xa)
  //     .attemptSql
