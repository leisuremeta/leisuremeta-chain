package io.leisuremeta.chain.lmscan.agent.model

final case class NodeStatus(
    networkId: Long,
    genesisHash: String,
    bestHash: String,
    number: Long,
)
