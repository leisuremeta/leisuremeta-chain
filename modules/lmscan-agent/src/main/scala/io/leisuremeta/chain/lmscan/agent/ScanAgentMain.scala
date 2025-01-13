package io.leisuremeta.chain.lmscan.agent

import cats.effect._

object ScanAgentMain extends IOApp:
  def build() =
    given PrintGreet()
    val program = for
      a <- (Korean.gen().run.foreverM).start
      b <- (English.gen().run.foreverM).start
      x <- a.join
      y <- b.join
    yield (x, y)
    program.toResource

  def run(args: List[String]): IO[ExitCode] = 
    val p = build()
    p.onCancel(IO.println("End").toResource).useForever
