package io.leisuremeta.chain.lmscan
package backend2
import doobie.*
import doobie.implicits.*
import doobie.util.ExecutionContexts

import io.leisuremeta.chain.lmscan.common.model.dao.Tx
import io.leisuremeta.chain.lmscan.common.model.dao.Account
import com.typesafe.config.ConfigFactory
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.common.model.dto.DTO_Account
import io.leisuremeta.chain.lmscan.backend2.Log.log2
import cats.instances.boolean
// import io.getquill

val config = ConfigFactory.load()
val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
  config.getString("ctx.db_className"), // driver classname
  config.getString("ctx.db_url"),       // connect URL (driver-specific)
  config.getString("ctx.db_user"),      // user
  config.getString("ctx.db_pass"),      // password
)

object QueriesFunction:
  def take[F](l: Int)(d: fs2.Stream[doobie.ConnectionIO, F]) = d.take(l)

  def drop[F](l: Int)(d: fs2.Stream[doobie.ConnectionIO, F]) = d.drop(l)

  def filter[F](f: String)(d: fs2.Stream[doobie.ConnectionIO, F]) =
    d.filter(d => true)

  // def hash[F](f: F => Boolean)(d: fs2.Stream[doobie.ConnectionIO, F]) =
  //   d.filter(d => d.hash == hash)

  // d.filter(f)

  def getPipeFunction[F](
      pipeString: String,
  ): fs2.Stream[doobie.ConnectionIO, F] => fs2.Stream[doobie.ConnectionIO, F] =
    pipeString match
      case s"take($number)" => take[F](number.toInt)
      case s"drop($number)" => drop[F](number.toInt)
      // case s"hash($hash)"   => hash[F]()
      case _ => filter[F]("true")

  def pipeRun[F](list: List[String])(
      acc: fs2.Stream[doobie.ConnectionIO, F],
  ): fs2.Stream[doobie.ConnectionIO, F] =
    list.length == 0 match
      case true => acc
      case false =>
        acc
          .pipe(getPipeFunction(list.head))
          .pipe(pipeRun[F](list.tail))

  def genPipeList(pipe: Option[String]) =
    pipe
      .getOrElse("")
      .split(",")
      .toList

object Queries:
  import QueriesFunction.*

  // https://devscan.leisuremeta.io/transactions/1 == https://devscan.leisuremeta.io/transaction/7fdc83bf729874a710af9dcab1c97d04daa89e96e378d7c44679f3c182d4b2ac
  // https://devscan.leisuremeta.io/transaction/7fdc83bf729874a710af9dcab1c97d04daa89e96e378d7c44679f3c182d4b2ac ==
  def getTxPipe[F](pipeString: Option[String]) =
    sql"select * from tx  ORDER BY  block_number DESC, event_time DESC  "
      .query[Tx] // DAO
      .stream
      .pipe(
        // common
        pipeString
          .pipe(genPipeList)
          .pipe(pipeRun),
      )
      .pipe(a => a)
      .take(100)
      // .filter(tx =>
      //   tx.hash == "7fdc83bf729874a710af9dcab1c97d04daa89e96e378d7c44679f3c182d4b2ac",
      // )
      // .filter(t => t.blockNumber == 2.pipe(a => a))
      .compile
      .toList
      .transact(xa)
      .attemptSql

  def getTx =
    sql"select * from tx"
      .query[Tx] // DAO
      .stream
      .filter(t => t.blockNumber == 2.pipe(a => a))
      .take(2)
      .compile // commont option
      .toList
      .transact(xa)
      .attemptSql

  // https://scan.leisuremeta.io/account/playnomm/

  // account detail => account
  // TX를 account 로 가져오기
  // https://scanapi.leisuremeta.io/tx/list?useDataNav=true&pageNo=0&sizePerRequest=1000&accountAddr=1f22f1e1571f70b7441480c8e4536dc4d5e65e27
  // useDataNav=true
  // &pageNo=0
  // &sizePerRequest=100
  // &accountAddr=1f22f1e1571f70b7441480c8e4536dc4d5e65e27

  def getTx_byAddress =
    sql"select * from tx"
      .query[Tx] // DAO
      .stream
      // .filter(t => t.blockNumber == 3.pipe(a => a))
      .filter(t =>
        // t.fromAddr == "eth-gateway" || t.toAddr.contains("eth-gateway"),
        t.fromAddr == "playnomm" || t.toAddr.contains("playnomm"),
      )
      .take(2)
      .compile // commont option
      .toList
      .transact(xa)
      .attemptSql
    // .filter(t =>
    //   // t.fromAddr == "eth-gateway" || t.toAddr.contains("eth-gateway"),
    //   t.fromAddr == "playnomm" || t.toAddr.contains("playnomm"),
    // )
    // .filter(t => t.displayYn == true)
    // .filter(t => t.blockNumber == 2.pipe(a => a))
    // .filter(t => false)
    // .sortBy(t => (t.blockNumber, t.eventTime))(Ord(Ord.desc, Ord.desc))
    // .drop(offset)
    // .take(sizePerRequest)

  def getAccount =
    sql"select * from account"
      .query[Account] // DAO
      .stream
      .take(1)
      .compile // commont option
      .toList
      .transact(xa)
      .attemptSql

  // def get[F[_]: Async](
  //     addr: String,
  // ): EitherT[F, String, Option[Account]] =
  //   inline def detailQuery =
  //     quote { (addr: String) =>
  //       query[Account]
  //         .join(query[AccountBalance])
  //         .on((a, b) => a.address == b.address)
  //         .filter(_._1.address == addr)
  //         .take(1)
  //         .map { case (a, b) =>
  //           Account(
  //             address = a.address,
  //             balance = b.balance,
  //             amount = a.amount,
  //             createdAt = a.createdAt,
  //           )
  //         }
  //     }
  //   optionQuery(detailQuery(lift(addr)))
