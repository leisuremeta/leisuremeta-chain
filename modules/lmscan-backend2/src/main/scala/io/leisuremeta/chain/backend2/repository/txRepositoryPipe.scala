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

  def filterTxHash(str: String)(
      d: Stream[ConnectionIO, DAO.Tx],
  ) =
    d.filter(tx => tx.hash == str)

  def filterBlockHash(str: String)(
      d: Stream[ConnectionIO, DAO.Tx],
  ) =
    d.filter(tx => tx.blockHash == str)

  def filterAddr(str: String)(
      d: Stream[ConnectionIO, DAO.Tx],
  ) =
    d.filter(tx => tx.fromAddr == str || tx.toAddr.contains(str))

  def filterSubtype(str: String)(
      d: Stream[ConnectionIO, DAO.Tx],
  ) =
    d.filter(tx => tx.subType == str)

  def filterDisplay(str: String)(
      d: Stream[ConnectionIO, DAO.Tx],
  ) =
    d.filter(tx => tx.displayYn == str.toBoolean)

  def getPipeFunctionTx(
      pipeString: String,
  ): Stream[ConnectionIO, DAO.Tx] => Stream[ConnectionIO, DAO.Tx] =
    pipeString match
      case s"take($str)"      => take(str.toInt)
      case s"drop($str)"      => drop(str.toInt)
      case s"hash($str)"      => filterTxHash(str)
      case s"blockHash($str)" => filterBlockHash(str)
      case s"addr($str)"      => filterAddr(str)
      case s"subtype($str)"   => filterSubtype(str)
      case s"display($str)"   => filterSubtype(str)
      case _                  => filterSelf

      // .filter(t => t.blockHash == lift(blockHash))
      // .filter(t => t.display_yn == true)

  def pipeRun(list: List[String])(
      acc: Stream[ConnectionIO, DAO.Tx],
  ): Stream[ConnectionIO, DAO.Tx] =
    list.length == 0 match
      case true => acc
      case false =>
        acc
          .pipe(getPipeFunctionTx(list.head))
          .pipe(pipeRun(list.tail))
