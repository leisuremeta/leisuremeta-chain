package io.leisuremeta.chain.lmscan.frontend
import scala.scalajs.js
import io.leisuremeta.chain.lmscan.common.model.*

case class ViewCase(
    var blockInfo: List[BlockInfo] = List(new BlockInfo),
    var txInfo: List[TxInfo] = List(new TxInfo),
    var nftInfo: List[NftActivity] = List(new NftActivity),
);

case class PageResponseViewCase(
    var block: PageResponse[BlockInfo] =
      PageResponse[BlockInfo](1, 1, Range(1, 10).map(d => new BlockInfo)),
    var tx: PageResponse[TxInfo] =
      PageResponse[TxInfo](1, 1, Range(1, 10).map(d => new TxInfo)),
    var board: SummaryModel = new SummaryModel,
    var blockDetail: BlockDetail = new BlockDetail,
    var txDetail: TxDetail = new TxDetail,
    var accountDetail: AccountDetail = new AccountDetail,
    var nftDetail: NftDetail = new NftDetail,
);
