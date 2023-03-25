package io.leisuremeta.chain.lmscan.frontend
import scala.scalajs.js
import io.leisuremeta.chain.lmscan.common.model.*

case class ViewCase(
    var blockInfo: List[BlockInfo] = List(new BlockInfo),
    var txInfo: List[TxInfo] = List(new TxInfo),
);

case class PageResponseViewCase(
    var block: PageResponse[BlockInfo] =
      new PageResponse[BlockInfo](0, 0, List()),
    var tx: PageResponse[TxInfo] = new PageResponse[TxInfo](0, 0, List()),
    var board: SummaryModel = new SummaryModel,
    var blockDetail: BlockDetail = new BlockDetail,
    var txDetail: TxDetail = new TxDetail,
    var accountDetail: AccountDetail = new AccountDetail,
);
