package org.leisuremeta.lmchain.core

import scala.concurrent.Future
import cats.effect.{ContextShift, IO}

package object client {

  implicit class FutureOps[A](val f: Future[A]) extends AnyVal {
    def toIO(implicit cs: ContextShift[IO]): IO[A] = IO.fromFuture(IO(f))
  }
}
