package io.leisuremeta.chain.lmscan.common.model.dto

// import io.leisuremeta.chain.lmscan.common.model.dto.DTO_Tx

final case class DTO_AccountDetail(
    address: Option[String] = None,
    balance: Option[BigDecimal] = None,
    value: Option[BigDecimal] = None,
    txList: Option[List[DTO_Tx]] = None,
)
