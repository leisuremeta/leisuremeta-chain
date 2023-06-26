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

object TxQueryPipe:
  import CommonQueriesFunction.*

  def getPipeFunctionTx[T](
      pipeString: String,
  ): Stream[ConnectionIO, T] => Stream[ConnectionIO, T] =
    pipeString match
      case s"take($number)" => take(number.toInt)
      case s"drop($number)" => drop(number.toInt)
      case s"hash($str)"    => filterTxHash(str)
      case _                => filterSelf

  def pipeRun[T](list: List[String])(
      acc: Stream[ConnectionIO, T],
  ): Stream[ConnectionIO, T] =
    list.length == 0 match
      case true => acc
      case false =>
        acc
          .pipe(getPipeFunctionTx(list.head))
          .pipe(pipeRun(list.tail))
