package io.leisuremeta.chain.lmscan.frontend
trait PubCase:
  def page: Int

object PubCase:
  case class txPub(page: Int)            extends PubCase
  case class blockPub(page: Int)         extends PubCase
  case class txDetailPub(page: Int)      extends PubCase
  case class accountDetailPub(page: Int) extends PubCase
  case class nftDetailPub(page: Int)     extends PubCase
  case class blockDetailPub(page: Int)   extends PubCase
  case class NonePub(page: Int = 1)      extends PubCase
