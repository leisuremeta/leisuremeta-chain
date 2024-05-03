package io.leisuremeta.chain.lmscan.frontend
object V:
  def validNull = (value: Option[String]) =>
    value match
      case Some("") => None
      case _        => value

  def commaNumber = (value: String) =>
    String.format(
      "%,d",
      value.replace("-", "0"),
    )
  def getOptionValue[T] = (field: Option[T], default: T) =>
    field match
      case Some(value) => value
      case None        => default

  def getOptionValuePipe[T](default: T)(field: Option[T]) =
    field match
      case Some(value) => value
      case None        => default

  def plainStr(
      data: Option[String] | Option[Int] | Option[Double] | Option[Long],
  ) =
    getOptionValue(data, "-").toString()

  def plainInt(data: Option[Int]) =
    getOptionValue(data, 0).asInstanceOf[Int].toString

  def plainLong(data: Option[Long]) =
    getOptionValue(data, 0.toLong).asInstanceOf[Long].toString

  def plainDouble(data: Option[Double]) =
    getOptionValue(data, 0.toDouble).asInstanceOf[Double].toString

  def plainSeason(data: Option[String]) =
    data match
      case Some(v) => if("""\d+\.?\d*""".r.matches(v)) s"SEASON $v:" else s"$v:"
      case _ => ""

  // def hash10(data: Option[Any]) =
  //   getOptionValue(data, "-").toString().take(10) + "..."

  def rarity(data: Option[String]) =
    getOptionValue(data, "-") match
      case "NRML" => "Normal"
      case "LGDY" => "Legendary"
      case "UNIQ" => "Unique"
      case "EPIC" => "Epic"
      case "RARE" => "Rare"
      case _ =>
        getOptionValue(data, "-").toString()

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
    data match
      case Some(str) => accountMatcher(str)
      case None => ""
    
  def accountMatcher(data: String) =
    data match
      case "playnomm" => "010cd45939f064fd82403754bada713e5a9563a1"
      case "reward-posting" => "d2c442e460e06d652f1d7c8706fd649306a5b9ce"
      case "reward-activity" => "43a57958149a577cd7528f6d79adbc5ba728c9f3"
      case "DAO-M" => "37cd3566cb27e40efdbdb8bf3e8264e7bd1ffffa"
      case "DAO-RWD" => "8010b03a46dd4519965796c011b36d37f841157d"
      case "DAO-REWARD" => "293b1e3cbb57ac8c354456d79a1e9675781650ed"
      case "creator-reward-posting" => "dad9764447ebe3e363cb383cb114aeedc442447c"
      case "creator-reward-activity" => "dca74dec332357ce717ba7702b8421edab2eaeee"
      case "reward-nft" => "2bc3ac647d09f47d1c733d28b4d151313d62864b"
      case "creator-rewar-fixqty" => "f7b90eed2a28d2d41a7ded5e66427b035be0fe9b"
      case "moonlabs" => "ec09ba30ac1038c91fa1ae587fbdf859557cbed1"
      case "eth-gateway" => "ca79f6fb199218fa681b8f441fefaac2e9a3ead3"
      case _ => data
