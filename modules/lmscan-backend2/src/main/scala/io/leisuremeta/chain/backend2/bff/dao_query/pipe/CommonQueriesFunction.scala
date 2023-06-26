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

object CommonQueriesFunction:
  def take[T](l: Int)(d: Stream[ConnectionIO, T]) = d.take(l)

  def drop[T](l: Int)(d: Stream[ConnectionIO, T]) = d.drop(l)

  def filterSelf[T](
      d: Stream[ConnectionIO, T],
  ) =
    d.filter(d => true)

  def filterTxHash[T](str: String)(
      d: Stream[ConnectionIO, T],
  ) =
    d.filter(d => true)
