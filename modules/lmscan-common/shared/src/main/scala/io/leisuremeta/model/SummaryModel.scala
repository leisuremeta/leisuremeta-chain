package io.leisuremeta.chain.lmscan.common.model

final case class SummaryModel(
    lmPrice: Option[Double] = None,
    blockNumber: Option[Long] = None,
    totalAccounts: Option[Long] = None,
    createdAt: Option[Long] = None,
    totalTxSize: Option[Long] = None,
    totalBalance: Option[BigDecimal] = None,
    marketCap: Option[BigDecimal] = None,
    cirSupply: Option[BigDecimal] = None,
    totalNft: Option[Long] = None,
) extends ApiModel 

final case class SummaryBoard(
    today: SummaryModel = SummaryModel(),
    yesterday: SummaryModel = SummaryModel(),
) extends ApiModel

final case class SummaryChart(
    list: Seq[SummaryModel] = Seq()
) extends ApiModel
