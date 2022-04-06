package org.leisuremeta.lmchain.core
package model

import datatype.BigNat

final case class NodeStatus(
    networkId: NetworkId,
    genesisHash: Block.BlockHash,
    bestHash: Block.BlockHash,
    number: BigNat,
)
