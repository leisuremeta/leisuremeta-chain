package io.leisuremeta.chain.lmscan.frontend
trait PubCase_M1:
  def data: String

object PubCase_M1:
  case class tx(data: String)            extends PubCase_M1
  case class block(data: String)         extends PubCase_M1
  case class txDetail(data: String)      extends PubCase_M1
  case class accountDetail(data: String) extends PubCase_M1
  case class nftDetail(data: String)     extends PubCase_M1
  case class blockDetail(data: String)   extends PubCase_M1
  case class None(data: String = "")     extends PubCase_M1
