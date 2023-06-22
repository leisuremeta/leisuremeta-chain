# flow

```scala


Queries.getTx // 쿼리 생성
|> QueriesPipe.pipeTx // 쿼리실헹 |> DTO 적용
|> ErrorHandle.genMsg // 에러핸들
|> .value // 값

```

#

```scala
object QueriesFunctionTest:
  type Q = Tx | Account

  def take(l: Int)(d: fs2.Stream[doobie.ConnectionIO, Q]) = d.take(l)

  def drop(l: Int)(d: fs2.Stream[doobie.ConnectionIO, Q]) = d.drop(l)

  def filter(f: String)(d: fs2.Stream[doobie.ConnectionIO, Q]) =
    d.filter(d => true)

  def hash(f: Q => Boolean)(d: fs2.Stream[doobie.ConnectionIO, Q]) =
    // d.filter(d => d.hash == hash)
    d.filter(f)

  def getPipeFunction(
      pipeString: String,
  ): fs2.Stream[doobie.ConnectionIO, Q] => fs2.Stream[doobie.ConnectionIO, Q] =
    pipeString match
      case s"take($number)" => take(number.toInt)
      case s"drop($number)" => drop(number.toInt)
      case s"hash($hash)" =>
        hash(a =>
          a match
            case d: Tx      => d.hash == hash
            case d: Account => d.address.get == hash,
        )
      case _ => filter("true")

  def pipeRun(list: List[String])(
      acc: fs2.Stream[doobie.ConnectionIO, Q],
  ): fs2.Stream[doobie.ConnectionIO, Q] =
    list.length == 0 match
      case true => acc
      case false =>
        acc
          .pipe(getPipeFunction(list.head))
          .pipe(pipeRun(list.tail))

  def genPipeList(pipe: Option[String]) =
    pipe
      .getOrElse("")
      .split(",")
      .toList

```

# tx list

```scala
  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[Tx]] =
    // OFFSET 시작번호, limit 페이지보여줄갯수
    val cntQuery = quote {
      query[Tx]
    }

    inline def pagedQuery =
      quote { (pageNavInfo: PageNavigation) =>
        val sizePerRequest = pageNavInfo.sizePerRequest
        val offset         = sizePerRequest * pageNavInfo.pageNo

        query[Tx]
          .sortBy(t => (t.blockNumber, t.eventTime))(Ord(Ord.desc, Ord.desc))
          .drop(offset)
          .filter(t => t.display_yn == true)
          .take(sizePerRequest)
      }

    val res = for
      totalCnt <- countQuery(cntQuery)
      payload  <- seqQuery(pagedQuery(lift(pageNavInfo)))
    yield (totalCnt, payload)

    res.map { (totalCnt, payload) =>
      val totalPages = calTotalPage(totalCnt, pageNavInfo.sizePerRequest)
      new PageResponse(totalCnt, totalPages, payload)
    }
```

```scala
// todo
// http://localhost:8081/tx?pipe=(take(3),absend,asd,asd,asd)&dto=(txDetailpage)&view=(form)


```

```scala
package io.leisuremeta.chain.lmscan
package backend2

import io.circe.generic.auto.*

import cats.Monad
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import cats.effect.std.Dispatcher
import cats.syntax.either.*
import cats.syntax.functor.toFunctorOps

import com.linecorp.armeria.server.Server
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.armeria.cats.ArmeriaCatsServerInterpreter

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.{*, given}

import common.LmscanApi
import common.ExploreApi
import common.model.{PageNavigation, SummaryModel}

// import io.leisuremeta.chain.lmscan.backend.service.*
import io.leisuremeta.chain.lmscan.backend2

import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.cors.CorsService;
import com.linecorp.armeria.server.annotation.Get
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.server.annotation.decorator.CorsDecorator;
import com.linecorp.armeria.server.DecoratingHttpServiceFunction
import sttp.tapir.server.armeria.TapirService
import sttp.tapir.server.armeria.cats.ArmeriaCatsServerOptions
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.interceptor.cors.CORSConfig
import io.leisuremeta.chain.lmscan.common.ExploreApi.baseEndpoint
import io.leisuremeta.chain.lmscan.common.model.dto.*
import io.circe.Encoder.AsArray.importedAsArrayEncoder
import doobie.*
import doobie.implicits.*
import doobie.util.ExecutionContexts
import cats.*
import cats.data.*
import cats.effect.*
import cats.implicits.*
import fs2.Stream

import sttp.tapir.model.StatusCodeRange.ServerError
import io.leisuremeta.chain.lmscan.common.ExploreApi.UserError
import io.leisuremeta.chain.lmscan.common.ExploreApi.ServerError
import io.leisuremeta.chain.lmscan.common.model.dao.Tx
import io.leisuremeta.chain.lmscan.common.model.Dao2Dto
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.backend2.CatsUtil.genEither
import io.leisuremeta.chain.lmscan.backend2.CatsUtil.eitherToEitherT
import io.leisuremeta.chain.lmscan.common.model.dao.Account
import io.leisuremeta.chain.lmscan.common.model.dto.DTO_Account
import io.leisuremeta.chain.lmscan.common.model.AccountDetail

object BackendMain extends IOApp:

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def tx[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    baseEndpoint.get
      .in("tx")
      .out(jsonBody[List[DTO_Tx]])
      .serverLogic { (Unit) => // Unit 대신에 프론트에서 url 함수 넣을수 있게 할수있다.
        scribe.info(s"get tx")
        Queries.getTx
          .pipe(QueriesPipe.pipeTx[F])
          .pipe(ErrorHandle.genMsg)
          .value
      }
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def tx2[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    baseEndpoint.get
      .in("tx2")
      .out(jsonBody[List[DTO_Tx]])
      .serverLogic { (Unit) => // Unit 대신에 프론트에서 url 함수 넣을수 있게 할수있다.
        scribe.info(s"get tx2")
        Queries.getTx_byAddress
          .pipe(QueriesPipe.pipeTx[F])
          .pipe(ErrorHandle.genMsg)
          .value
      }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def account[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    baseEndpoint.get
      .in("account")
      .out(jsonBody[Account])
      .serverLogic { (Unit) => // Unit 대신에 프론트에서 url 함수 넣을수 있게 할수있다.
        scribe.info(s"get Account")
        Queries.getAccount
          .pipe(QueriesPipe.pipeAccount[F])
          .pipe(ErrorHandle.genMsg)
          .value
      }

  def accountService[F[_]: Async] =
    val r = for
      account <- Queries.getAccount
        .pipe(QueriesPipe.pipeAccount[F])
      txList <- Queries.getTx.pipe(QueriesPipe.pipeTx[F])
    yield (account, txList)
    r.map { (account, txList) =>
      DTO_AccountDetail(
        address = account.address,
        balance = account.balance,
        value = account.balance,
        txList = Some(txList),
      )
    }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def accountDetail[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    baseEndpoint.get
      .in("account")
      .in("detail")
      .out(jsonBody[DTO_AccountDetail])
      .serverLogic { (Unit) => // Unit 대신에 프론트에서 url 함수 넣을수 있게 할수있다.
        scribe.info(s"get Account")
        accountService
          .pipe(ErrorHandle.genMsg)
          .value
      }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def explorerEndpoints[F[_]: Async]: List[ServerEndpoint[Fs2Streams[F], F]] =
    List(
      tx[F],
      tx2[F],
      account[F],
      accountDetail[F],
    )

  def getServerResource[F[_]: Async]: Resource[F, Server] =
    for
      dispatcher <- Dispatcher.parallel[F]
      server <- Resource.make(Async[F].async_[Server] { cb =>

        val options = ArmeriaCatsServerOptions
          .customiseInterceptors(dispatcher)
          .corsInterceptor(Some {
            CORSInterceptor
              .customOrThrow[F](CORSConfig.default)
          })
          .options

        val tapirService = ArmeriaCatsServerInterpreter[F](options)
          .toService(explorerEndpoints[F])
        val server = Server.builder
          .annotatedService(tapirService)
          .http(8081)
          .maxRequestLength(128 * 1024 * 1024)
          .requestTimeout(java.time.Duration.ofMinutes(6))
          .service(tapirService)
          .build
        server.start.handle[Unit] {
          case (_, null)  => cb(Right(server))
          case (_, cause) => cb(Left(cause))
        }
      }) { server =>
        Async[F]
          .fromCompletableFuture(Async[F].delay(server.closeAsync()))
          .void
      }
    yield server

  override def run(
      args: List[String],
  ): IO[ExitCode] =

    val program: Resource[IO, Server] =
      for server <- getServerResource[IO]
      yield server

    program.useForever.as(ExitCode.Success)
```