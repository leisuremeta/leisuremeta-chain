package io.leisuremeta.chain.lmscan.common.model

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

object NodeValidator:
  case class Validator(
      address: Option[String] = None,
      power: Option[Double] = None,
      cnt: Option[Long] = None,
      name: Option[String] = None,
  ) extends ApiModel 
  case class ValidatorList(
    payload: List[Validator] = Nil,
  ) extends ApiModel
  case class ValidatorDetail(
    validator: Validator = Validator(),
    page: PageResponse[BlockInfo] = PageResponse(),
  ) extends ApiModel
  given Decoder[Validator] = deriveDecoder[Validator]
  given Decoder[ValidatorList] = deriveDecoder[ValidatorList]
  given Decoder[ValidatorDetail] = deriveDecoder[ValidatorDetail]
