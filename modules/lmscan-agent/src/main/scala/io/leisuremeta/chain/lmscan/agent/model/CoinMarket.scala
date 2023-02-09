package io.leisuremeta.chain.lmscan.agent.model


final case class LmPrice(
  status: Status,
  data: Id,
)

final case class Status(
  error_code: Int,
  error_message: Option[String],
)

final case class Id(
  `21315`: Data
)

final case class Data(
  id: Long,
  name: String,
  symbol: String,
  last_updated: String,
  quote: Currency,
)

final case class Currency(
  `USD`: USDCurrency
)

final case class USDCurrency(
  price: Double,
  last_updated: String,
)

