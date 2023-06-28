package io.leisuremeta.chain.lmscan
package backend2
import doobie.*
import doobie.implicits.*
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.common.model.DAO
import io.leisuremeta.chain.lmscan.backend2.CommonPipe.*
import io.leisuremeta.chain.lmscan.backend2.Log.log2

object BlockRepository:
  import BlockRepositoryPipe.*
  def getBlockPipeAsync(pipeString: Option[String]) =
    sql"select * from block  ORDER BY  number DESC  "
      .query[DAO.Block]
      .stream
      .pipe(
        pipeString
          .pipe(genPipeList)
          .pipe(pipeRun),
      )
      .compile
      .toList
