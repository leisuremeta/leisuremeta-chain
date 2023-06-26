package io.leisuremeta.chain.lmscan
package backend2
import doobie.*
import doobie.implicits.*
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.common.model.DAO
import io.leisuremeta.chain.lmscan.backend2.CommonPipe.*

object TxQuery:
  import TxQueryPipe.*

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
