package org.leisuremeta.lmchain.core
package node
package endpoint

import cats.effect.Sync
import cats.implicits._

import io.circe.generic.auto._
import io.circe.refined._
import io.finch._
import io.finch.circe._

import codec.circe._
import model.Signed
import repository.TransactionRepository
import service.{LocalGossipService, TransactionService}

object TransactionEndpoint {

  def Get[F[_]: Sync: TransactionRepository: LocalGossipService](implicit
      finch: EndpointModule[F]
  ): Endpoint[F, Signed.Tx] = {

    import finch._

    get("transaction" :: path[Signed.TxHash].withToString("{transactionHash}"))
      .mapOutputAsync { (transactionHash: Signed.TxHash) =>
        scribe.debug(s"Receive get transaction request: $transactionHash")
        TransactionService.get[F](transactionHash).value.map {
          case Right(Some(tx)) => Ok(tx)
          case Right(None) =>
            NotFound(new Exception(s"Not found: $transactionHash"))
          case Left(errorMsg) =>
            scribe
              .info(
                s"Get transaction $transactionHash error response: $errorMsg"
              )
            InternalServerError(new Exception(errorMsg))
        }
      }
  }

  def Post[F[_]: Sync: LocalGossipService](
      implicit finch: EndpointModule[F]
  ): Endpoint[F, Signed.TxHash] = {

    import finch._

    post("transaction" :: jsonBody[Signed.Tx]).mapOutputAsync {
      (t: Signed.Tx) =>
        scribe.debug(s"Receive post transaction request: $t")
        TransactionService.submit[F](t).value.map {
          case Right(txHash) => Ok(txHash)
          case Left(errorMsg) =>
            scribe.debug(s"Post transaction $t error response: $errorMsg")
            InternalServerError(new Exception(errorMsg))
        }
    }
  }
}
