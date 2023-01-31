package io.leisuremeta.chain.lmscan.agent.model

import io.leisuremeta.chain.lmscan.agent.model.PHeader

final case class PBlock(
  header: PHeader,
  transactionHashes: Seq[String],
  votes: Seq[Signature]
)
