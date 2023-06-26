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

object QueriesFunctionCommon:
  def take[T](l: Int)(d: Stream[ConnectionIO, T]) = d.take(l)

  def drop[T](l: Int)(d: Stream[ConnectionIO, T]) = d.drop(l)

object QueriesFunction:
  import QueriesFunctionCommon.*

  def filterTxHash(str: String)(
      d: Stream[ConnectionIO, DAO.Tx],
  ) =
    d.filter(d => d.hash == str)

  def filterSelf[T](
      d: Stream[ConnectionIO, T],
  ) =
    d.filter(d => true)

  def getPipeFunctionTx(
      pipeString: String,
  ): Stream[ConnectionIO, DAO.Tx] => Stream[ConnectionIO, DAO.Tx] =
    pipeString match
      case s"take($number)" => take(number.toInt)
      case s"drop($number)" => drop(number.toInt)
      case s"hash($str)"    => filterTxHash(str)
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

  def genPipeList(pipe: Option[String]) =
    pipe
      .getOrElse("")
      .split(",")
      .toList

object Queries:
  import QueriesFunction.*

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
