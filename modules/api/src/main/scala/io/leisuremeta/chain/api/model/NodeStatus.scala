package io.leisuremeta.chain
package api.model

import lib.datatype.BigNat

final case class NodeStatus(
    networkId: NetworkId,
    genesisHash: Block.BlockHash,
    bestHash: Block.BlockHash,
    number: BigNat,
)
