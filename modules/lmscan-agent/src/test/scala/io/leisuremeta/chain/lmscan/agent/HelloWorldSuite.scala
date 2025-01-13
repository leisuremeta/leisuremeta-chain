package io.leisuremeta.chain.lmscan.agent

import munit.CatsEffectSuite

class HelloWorldSuite extends CatsEffectSuite:
  test("안녕?"):
    given PrintGreet()
    for
      s <- Korean.gen().greeting
    yield assertEquals(s, "안녕")

  test("hello?"):
    given PrintGreet()
    for
      s <- English.gen().greeting
    yield assertEquals(s, "Hello")
