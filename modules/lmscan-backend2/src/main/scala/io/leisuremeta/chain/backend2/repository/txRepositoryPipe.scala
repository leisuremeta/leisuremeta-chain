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

object TxRepositoryPipe:
  import CommonQueriesFunction.*

  def filterTxHash[T](str: String)(
      d: Stream[ConnectionIO, DAO.Tx],
  ) =
    d.filter(tx => tx.hash == str)

  def filterTxAccount[T](str: String)(
      d: Stream[ConnectionIO, DAO.Tx],
  ) =
    d.filter(tx => tx.fromAddr == str || tx.toAddr.contains(str))

  def getPipeFunctionTx(
      pipeString: String,
  ): Stream[ConnectionIO, DAO.Tx] => Stream[ConnectionIO, DAO.Tx] =
    pipeString match
      case s"take($number)" => take(number.toInt)
      case s"drop($number)" => drop(number.toInt)
      case s"hash($str)"    => filterTxHash(str)
      case s"addr($str)"    => filterTxAccount(str)
      case _                => filterSelf

  def pipeRun(list: List[String])(
      acc: Stream[ConnectionIO, DAO.Tx],
  ): Stream[ConnectionIO, DAO.Tx] =
    list.length == 0 match
      case true => acc
      case false =>
        acc
          .pipe(getPipeFunctionTx(list.head))
          .pipe(pipeRun(list.tail))
