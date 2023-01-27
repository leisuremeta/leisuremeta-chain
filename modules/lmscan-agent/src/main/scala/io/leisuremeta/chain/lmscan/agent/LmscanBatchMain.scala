import cats.effect.IOApp
import cats.effect.IO
import cats.effect.ExitCode
import sttp.client3.armeria.cats.ArmeriaCatsBackend
import sttp.client3.SttpBackendOptions

import scala.concurrent.duration.*

object LmscanBatchMain extends IOApp:
  def run(args: List[String]): IO[ExitCode] =
    for _ <- ArmeriaCatsBackend
        .resource[IO](
          SttpBackendOptions.Default.connectionTimeout(10.minutes),
        )
        .use { backend =>
          ???
          checkLoop()
        }
    yield ExitCode.Success

  def checkLoop(): IO[Unit] = for
    _ <- IO.delay(scribe.info(s"data insertion started"))
    _ <- checkBlocks()
  yield ()

  def checkBlocks(): IO[Unit] = for _ <- IO.none // for preventing compile error
  yield ()
