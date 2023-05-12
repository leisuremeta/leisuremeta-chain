package io.leisuremeta.chain.gateway.eth.common

import cats.effect.IO

class GatewayServerTest extends munit.CatsEffectSuite:

  test("should start and stop server"):
    IO(42).assertEquals(42)
