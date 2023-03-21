package io.leisuremeta.chain.lmscan.frontend

import io.leisuremeta.chain.lmscan.frontend.Log.log
import io.leisuremeta.chain.lmscan.frontend.Builder.getPage
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.common.model.BlockInfo

object ReducePipe:
  // [PubCase]
  // |> [PubCase_m1] // api 단계에서 처리
  // |> [PubCase_m2] // parser 로직으로 처리
  // |> [PubCase_m2f1] // 필터 로직으로 처리
  // |> <PubCase_m1f1-r> // ReducePipe 로 처리
  // |> html(<m1>)

  def getBlocks = (model: Model) =>
    getPage(model.observers).pubs.reverse
      .filter(d =>
        d.pub_m2 match
          case block: PageResponse[BlockInfo] => true,
          // case _                          => false,
      )(0)
      .pub_m2
      .payload
      .toList
