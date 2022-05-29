package io.leisuremeta.chain.node.service

import scala.concurrent.duration._

import cats.effect._
import cats.effect.unsafe.implicits.global

import minitest.SimpleTestSuite
import hedgehog.minitest.HedgehogSupport
import hedgehog.*

object PeriodicActionServiceTest extends SimpleTestSuite with HedgehogSupport:

  example("nextBlockSuggestionTimeMillis") {

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

    next ==== expected
  }

  example("periodic") {

  // count            1  2  3  4  5  6  7  8  9 
  //            |--+--+--+--+--+--+--+--+--+--+--|
  // time(100ms)|  1  2  3  4  5  6  7  8  9  10 |

    var counter = 0


    scala.util.Try{
//      val time0 = System.currentTimeMillis
//      println(s"===> time0: $time0")
      PeriodicActionService
        .periodic[IO](200, 100)(IO{
//          println(s"time passed ${System.currentTimeMillis - time0} ms")
          counter += 1
        })
        .timeout(1050.millis)
        .unsafeRunSync()
    }

    counter ==== 9
  }
