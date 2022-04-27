package org.leisuremeta.lmchain.core
package node
package endpoint

import cats.effect.Async
import cats.implicits._

import io.finch._

import model.Block
import repository.BlockRepository
import service.{BlockService, LocalGossipService}

object BlockEndpoint {

  def Get[F[_]: Async: BlockRepository: LocalGossipService](implicit
      finch: EndpointModule[F]
  ): Endpoint[F, Block] = {

    import finch._

    get("block" :: path[Block.BlockHash].withToString("{blockHash}")) {
      (blockHash: Block.BlockHash) =>
        BlockService.get[F](blockHash).value.map {
          case Right(Some(block)) => Ok(block)
          case Right(None) => NotFound(new Exception(s"Not found: $blockHash"))
          case Left(errorMsg) =>
            scribe.debug(s"Get block $blockHash error response: $errorMsg")
            InternalServerError(new Exception(errorMsg))
        }
    }
  }
}
