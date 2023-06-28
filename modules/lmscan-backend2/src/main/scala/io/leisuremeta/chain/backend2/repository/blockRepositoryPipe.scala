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

object BlockRepositoryPipe:
  import CommonQueriesFunction.*

  def filterBlockHash(str: String)(
      d: Stream[ConnectionIO, DAO.Block],
  ) =
    d.filter(tx => tx.hash == str)

  def getPipeFunctionBlock(
      pipeString: String,
  ): Stream[ConnectionIO, DAO.Block] => Stream[ConnectionIO, DAO.Block] =
    pipeString match
      case s"take($str)" => take(str.toInt)
      case s"drop($str)" => drop(str.toInt)
      case s"hash($str)" => filterBlockHash(str)
      case _             => filterSelf

  def pipeRun(list: List[String])(
      acc: Stream[ConnectionIO, DAO.Block],
  ): Stream[ConnectionIO, DAO.Block] =
    list.length == 0 match
      case true => acc
      case false =>
        acc
          .pipe(getPipeFunctionBlock(list.head))
          .pipe(pipeRun(list.tail))
