package io.leisuremeta.chain.lmscan.frontend

import io.leisuremeta.chain.lmscan.frontend.Log.log
import io.leisuremeta.chain.lmscan.frontend.Builder.*
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.common.model.BlockInfo

object ReducePipe:
  def getPubM2 = (model: Model) => ""
  // getPage(model.observers) match
  //   case PageCase.Blocks(_, _, pubs, _) =>
  //     pubs.reverse
  //       .filter(d =>
  //         d match
  //           // case block: PageResponse[BlockInfo] => true
  //           case _ => true,
  //       )(0)
  //       .pub_m2
  //       .payload
  //       .toList
  //   case PageCase.Transactions(_, _, pubs, _) =>
  //     pubs.reverse
  //       .filter(d =>
  //         d match
  //           // case block: PageResponse[BlockInfo] => true
  //           case _ => true,
  //       )(0)
  //       .pub_m2
  //       .payload
  //       .toList

  // [PubCase]
  // |> [PubCase_m1] // api 단계에서 처리
  // |> [PubCase_m2] // parser 로직으로 처리
  // |> [PubCase_m2f1] // 필터 로직으로 처리
  // |> <PubCase_m1f1-r> // ReducePipe 로 처리
  // |> html(<m1>)
