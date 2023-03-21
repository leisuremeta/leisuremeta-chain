package io.leisuremeta.chain.lmscan.frontend
object Num:
  def Int_Positive = (v: Int) =>
    v < 1 match
      case true => 1
      case _    => v
