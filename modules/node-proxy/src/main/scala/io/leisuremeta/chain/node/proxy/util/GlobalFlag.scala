package io.leisuremeta.chain.node.proxy
package util

final case class GlobalFlag()

object GlobalFlag:
  @volatile private var flag = false

  val default = GlobalFlag()
  



