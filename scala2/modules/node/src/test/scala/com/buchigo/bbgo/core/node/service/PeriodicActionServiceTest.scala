package org.leisuremeta.lmchain.core.node.service

import scala.concurrent.duration._

import cats.effect._

class PeriodicActionServiceTest extends munit.FunSuite {

  test("nextBlockSuggestionTimeMillis") {

    val numberOfNodes    = 1
    val localNodeIndex   = 0
    val current          = 1635743169882L
    val timeWindowMillis = 10000L

    val next = PeriodicActionService.nextBlockSuggestionTimeMillis(
      numberOfNodes,
      localNodeIndex,
      current,
      timeWindowMillis,
    )

    val expected = (1635743169882L / 10000L) * 10000L + 10000L

    assertEquals(next, expected)
  }

  test("periodic") {

    implicit val ec: scala.concurrent.ExecutionContext =
      scala.concurrent.ExecutionContext.global
    implicit val cs: ContextShift[IO] = IO.contextShift(ec)
    implicit val timer                = IO.timer(ec)

    var counter = 0

    scala.util.Try(
      PeriodicActionService
        .periodic[IO](100, 100) { counter += 1 }
        .timeout(1050.millis)
        .unsafeRunSync()
    )

    assertEquals(counter, 9)
  }
}
