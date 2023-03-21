package io.leisuremeta.chain.lmscan.frontend

import io.leisuremeta.chain.lmscan.frontend.Log.log

object PubCase_Pipe:
  // [PubCase]
  // |> [PubCase_m1]
  // |> [PubCase_m1(f1)]
  // |> <PubCase_m1(f1)r1>
  // |> html(<m1>)

  def m1 = "a" // m1 역할을 PageMsg.DataUpdate 가 대신해주고 있다
  // def m1f1   = (pubs, pubs_m1) =>
  def m1r1 = (pubs: List[PubCase], pubs_m1: List[PubCase_M1]) =>
    // log()
    ""
