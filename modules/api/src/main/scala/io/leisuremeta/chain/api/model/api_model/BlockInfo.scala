package io.leisuremeta.chain
package api.model
package api_model

import java.time.Instant

import lib.datatype.BigNat

final case class BlockInfo(
    blockNumber: BigNat,
    timestamp: Instant,
    blockHash: Block.BlockHash,
    txCount: Int,
)
