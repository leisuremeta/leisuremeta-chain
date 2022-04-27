package org.leisuremeta.lmchain.core.failure

import scala.util.control.NoStackTrace

sealed trait LmChainFailure extends NoStackTrace {
  def msg: String
}

final case class EncodingFailure(msg: String) extends LmChainFailure
final case class DecodingFailure(msg: String) extends LmChainFailure
