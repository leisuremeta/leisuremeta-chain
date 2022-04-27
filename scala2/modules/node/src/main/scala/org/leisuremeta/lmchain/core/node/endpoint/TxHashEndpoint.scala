package org.leisuremeta.lmchain.core
package node
package endpoint

import cats.effect.Sync

import io.circe.generic.auto._
import io.circe.refined._
import io.finch._
import io.finch.circe._

import codec.circe._
import crypto.Hash.ops._
import model.Transaction

object TxHashEndpoint {

  def Post[F[_]: Sync](implicit finch: EndpointModule[F]): Endpoint[F, Transaction.TxHash] = {

    import finch._

    post("txhash" :: jsonBody[Transaction]).mapOutput {
      (t: Transaction) =>
        scribe.info(s"Receive post txhash request: $t")
        Ok(t.toHash)
    }
  }
}
