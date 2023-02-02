package io.leisuremeta.chain.lmscan.agent.repository

import cats.effect.kernel.Async
import io.leisuremeta.chain.lmscan.agent.entity.Tx
import cats.data.EitherT
import io.getquill.*
import cats.effect.IO
import io.getquill.Query
import io.getquill.context.*
import io.leisuremeta.chain.lmscan.agent.repository.CommonQuery
import cats.effect.IOApp
import sttp.client3.armeria.cats.ArmeriaCatsBackend
import sttp.client3.SttpBackendOptions
import cats.effect.ExitCode
import cats.effect.unsafe.implicits.global
import scala.concurrent.ExecutionContext
import java.time.Duration
import java.time.Instant
import io.leisuremeta.chain.lmscan.agent.model.id

case class Person(name: String, age: Int)

object TxRepository extends CommonQuery with IOApp:
  import ctx.{*, given}

  // def insert[F[_]: Async, T <: id](tx: T): EitherT[F, String, Long] =
  //   insertTransaction[F, T](tx)


  def run(args: List[String]): IO[cats.effect.ExitCode] =
    for _ <- ArmeriaCatsBackend
        .resource[IO](SttpBackendOptions.Default)
        .use { backend =>
          {

            val x =
              for count <- insertTransaction[IO, Tx](
                  Tx(
                    "123",
                    "account",
                    "token",
                    "123a",
                    Seq("111", "222"),
                    "xx123a",
                    112L,
                    Instant.now().getEpochSecond(),
                    Instant.now().getEpochSecond(),
                    Seq(
                      "4913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
                    ),
                    Seq(
                      "b775871c85faae7eb5f6bcebfd28b1e1b412235c/123456789.12345678912345678",
                      "b775871c85faae7eb5f6bcebfd28b1e1b412235c/123456789.12345678912345678",
                    ),
                    "sssssss",
                  ),
                )
              yield count
            println("ccccc : ")
            // println("zzzzzzzzzz : " + x.value.unsafeRunSync())
            scribe.info(s"count: ${x.value}")
            x
          }.value
        }
    yield ExitCode.Success
