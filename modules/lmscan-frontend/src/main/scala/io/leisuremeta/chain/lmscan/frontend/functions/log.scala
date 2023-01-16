package io.leisuremeta.chain.lmscan.frontend

// TODO:: make multiple input log function
object Log:
  def log[A](x: A): A =
    println(x); x

  // log(a,b,c,d,....z)
  // output : (a,b,c,...z)
  // return : z
  // return type : (same as) z type

  // example
  // log(1)    // output: (1) return: 1 : return type: Int
  // log(1, 2) // output: (1,2) return: 2 : return type: Int
