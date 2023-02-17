package io.leisuremeta.chain.lmscan.agent.model

import java.time.Instant

final case class BlockInfo(
  blockNumber: BigInt,
  timestamp: Instant,
  // blockHash: PBlock,
  txCount: Int,
)
