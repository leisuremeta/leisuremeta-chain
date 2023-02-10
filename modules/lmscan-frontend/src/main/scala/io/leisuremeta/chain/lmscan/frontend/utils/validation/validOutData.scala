package io.leisuremeta.chain.lmscan.frontend

enum V:
  case TxValue extends V

object ValidOutputData:
  def getOptionValue = (field: Option[Any], default: Any) =>
    field match
      case Some(value) => value
      case None        => default

  def vData(data: Option[Any], types: V): String = types match
    case V.TxValue =>
      val result = getOptionValue(data, "-").toString().length() match
        case 25 => getOptionValue(data, "-").toString()
        case _ =>
          getOptionValue(data, "-").toString().length() > 10 match
            case true =>
              getOptionValue(data, "-").toString().take(10) + "LM" // "overflow"
            case false => getOptionValue(data, "-").toString() + "LM"
      {
        result.length() match
          case 3 => "-"
          case _ => result
      }
