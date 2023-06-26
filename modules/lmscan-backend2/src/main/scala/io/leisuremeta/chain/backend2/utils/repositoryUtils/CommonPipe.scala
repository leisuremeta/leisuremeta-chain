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
import cats.implicits.catsSyntaxHash

object CommonPipe:
  import CommonQueriesFunction.*

  def genPipeList(pipe: Option[String]) =
    pipe
      .getOrElse("")
      .split(",")
      .toList

  def getPipeFunction[T](
      pipeString: String,
  ): Stream[ConnectionIO, T] => Stream[ConnectionIO, T] =
    pipeString match
      case s"take($number)" => take(number.toInt)
      case s"drop($number)" => drop(number.toInt)
      case _                => filterSelf

  def pipeRun[T](list: List[String])(
      acc: Stream[ConnectionIO, T],
  ): Stream[ConnectionIO, T] =
    list.length == 0 match
      case true => acc
      case false =>
        acc
          .pipe(getPipeFunction(list.head))
          .pipe(pipeRun(list.tail))
