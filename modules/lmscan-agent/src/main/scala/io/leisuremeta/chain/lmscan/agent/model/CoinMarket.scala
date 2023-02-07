package io.leisuremeta.chain.lmscan.agent.model


final case class LmPrice(
  status: Status,
  data: Data,
)

final case class Status(
  error_code: Int,
  error_message: String,
)

final case class Data(
  id: Long,
  name: String,
  symbol: String,
  last_updated: String,
  quote: USD,
)

final case class USD(
  price: Double,
  last_updated: String,
)

