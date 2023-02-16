package io.leisuremeta.chain.lmscan.frontend

import scala.util.matching.Regex

enum V:
  case TxValue extends V

object ValidOutputData:
  def getOptionValue = (field: Option[Any], default: Any) =>
    field match
      case Some(value) => value
      case None        => default

  def vData(data: Option[Any], types: V): String =
    types match
      case V.TxValue =>
        val res = String
          .format(
            "%.4f",
            (getOptionValue(data, "0.0")
              .asInstanceOf[String]
              .toDouble / Math.pow(10, 18).toDouble),
          )
        val sosu         = res.takeRight(5)
        val decimal      = res.replace(sosu, "")
        val commaDecimal = String.format("%,d", decimal.toDouble)

        res == "0.0000" match
          case true =>
            "-"
          case false => commaDecimal + sosu
