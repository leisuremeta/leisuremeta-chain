package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.parser.*
import io.circe.generic.auto.*
import io.leisuremeta.chain.api.model.{TransactionWithResult}

object TransactionWithResultParser:

  def decodeParser(data: String) = decode[TransactionWithResult](data)
  // def getParsedData(data: String) =
  //   decodeParser(data).getOrElse("디코딩 실패\n")
