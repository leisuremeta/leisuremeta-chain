package io.leisuremeta.chain.lmscan.frontend

object Log:
  def log[A](x: A): A =
    println(x); x
