package io.leisuremeta.chain.lmscan.frontend

object CommonFunc:
  def getFieldValue = (field: Option[Any]) => field match 
    case Some(value) => value.toString()
    case None => "-"
