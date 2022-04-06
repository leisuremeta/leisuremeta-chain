package org.leisuremeta.lmchain.core
package node
package endpoint

import cats.effect.Async
import cats.implicits._

import io.finch._

import model.{Block, NetworkId, NodeStatus}
import repository.BlockRepository
import service.LocalStatusService

object NodeStatusEndpoint {

  def Get[F[_]: Async: BlockRepository](
      networkId: NetworkId,
      genesisHash: Block.BlockHash,
  )(implicit finch: EndpointModule[F]): Endpoint[F, NodeStatus] = {

    import finch._

    get("status") {
      scribe.debug("status request")
      LocalStatusService.status[F](networkId, genesisHash).value.map {
        case Left(msg) =>
          scribe.debug(s"status response bad request: $msg")
          BadRequest(new Exception(msg))
        case Right(status) =>
          scribe.debug(s"status response: $status")
          Ok(status)
      }
    }
  }
}
