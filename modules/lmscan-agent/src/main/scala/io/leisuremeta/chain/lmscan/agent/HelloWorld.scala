package io.leisuremeta.chain.lmscan.agent

import cats.effect.IO
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import cats.effect.kernel.Resource

trait Greet[F[_]]:
  def greeting(s: String): F[String]
  def run(s: String, d: Long): F[Unit]

class PrintGreet() extends Greet[IO]:
  def greeting(s: String): IO[String] = IO(s)
  def run(s: String, d: Long): IO[Unit] = IO(s).andWait(Duration(d, TimeUnit.SECONDS)).flatMap(IO.println)

object PrintGreet:
  def apply: Resource[IO, PrintGreet] = Resource.pure(new PrintGreet())

trait Korean[F[_]]:
  def greeting: F[String]
  def run: F[Unit]

object Korean:
  val s = "안녕"
  val d = 2
  def gen[F[_]]()(using g: Greet[F]): Korean[F] = new Korean[F]:
    def greeting = g.greeting(s)
    def run = g.run(s, d)

trait English[F[_]]:
  def greeting: F[String]
  def run: F[Unit]

object English:
  val s = "Hello"
  val d = 1
  def gen[F[_]]()(using g: Greet[F]): English[F] = new English[F]:
    def greeting = g.greeting(s)
    def run = g.run(s, d)
