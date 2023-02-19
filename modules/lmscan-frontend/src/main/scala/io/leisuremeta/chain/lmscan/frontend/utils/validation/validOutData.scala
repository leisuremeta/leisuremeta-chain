package io.leisuremeta.chain.lmscan.frontend
object W:
  def getOptionValue = (field: Option[Any], default: Any) =>
    field match
      case Some(value) => value
      case None        => default

  def plainStr(data: Option[String]) =
    getOptionValue(data, "-").toString()

  def plainInt(data: Option[Int]) =
    getOptionValue(data, 0).asInstanceOf[Int].toString

  def hash10(data: Option[Any]) =
    getOptionValue(data, "-").toString().take(10) + "..."

  def txValue(data: Option[String]) =
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

  def accountHash(data: Option[String]) =
    plainStr(data).length match
      case 40 =>
        hash10(data)
      case _ =>
        plainStr(data).toString() match
          case "playnomm" =>
            hash10(Some("010cd45939f064fd82403754bada713e5a9563a1"))

          case "eth-gateway" =>
            hash10(Some("ca79f6fb199218fa681b8f441fefaac2e9a3ead3"))

          case _ =>
            plainStr(data)

  def _any(data: Option[Any]) = ""
