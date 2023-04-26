package io.leisuremeta.chain.node.proxy
package model

import io.leisuremeta.chain.lib.datatype.BigNat

final case class NodeConfig(
  blockNumber: Option[BigNat],
  oldNodeAddress: String,
  newNodeAddress: Option[String],
)
