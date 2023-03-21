package io.leisuremeta.chain.lmscan.frontend

object PipePub:
  // map => filter => reduce
  def m1     = "a"
  def m1f1   = "a"
  def m1f1r1 = "a"

// [pub]  -- [pub]
// |> map_pub  -- [sub]
// |> filter_pub  -- [filter sub]
// |> reduce1_pub  -- sub
