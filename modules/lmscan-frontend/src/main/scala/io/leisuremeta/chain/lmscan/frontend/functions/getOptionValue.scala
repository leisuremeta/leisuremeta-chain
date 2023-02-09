package io.leisuremeta.chain.lmscan.frontend

object CommonFunc:
  def getOptionValue = (field: Option[Any], default: Any) => field match 
    case Some(value) => value
    case None => default
