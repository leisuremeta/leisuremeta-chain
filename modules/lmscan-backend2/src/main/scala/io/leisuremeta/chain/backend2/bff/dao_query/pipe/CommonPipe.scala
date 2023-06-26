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

  def genPipeList(pipe: Option[String]) =
    pipe
      .getOrElse("")
      .split(",")
      .toList
