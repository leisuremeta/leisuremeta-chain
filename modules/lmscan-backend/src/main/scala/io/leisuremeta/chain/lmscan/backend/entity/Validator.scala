package io.leisuremeta.chain.lmscan.backend.entity

import io.leisuremeta.chain.lmscan.common.model.NodeValidator

final case class ValidatorInfo(
    address: String,
    power: Option[Double],
    cnt: Option[Long],
    name: Option[String],
):
    def toModel = NodeValidator.Validator(
        Some(address),
        power,
        cnt,
        name,
    )
