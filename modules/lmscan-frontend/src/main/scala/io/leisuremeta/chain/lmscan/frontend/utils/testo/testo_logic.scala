package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.Log.*
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.frontend.TestoLogic.plainInt
import java.math.RoundingMode

object TestoLogic:
//{
//     "id": 9124,
//     "lmPrice": 0.02836384,
//     "blockNumber": 1774948,
//     "totalTxSize": 842923267,
//     "totalAccounts": 230378,
//     "createdAt": 1685515435
// }
  val formatter = java.text.NumberFormat.getNumberInstance()
  formatter.setRoundingMode(RoundingMode.FLOOR)
  formatter.setMaximumFractionDigits(4)

  val plainInt              = 1234512345
  val maybe_plainInt_string = "1234512345"
  val maybe_plainInt_broken = "123451234X"

  val plainLong = 1234512345L

  val plainDouble     = 12345.12345
  val max_plainDouble = 12345.123451234512

  val plainBigDecimal     = BigDecimal("12345123451234512345")
  val max_plainBigDecimal = BigDecimal(12345.123451234512)

  def any2Str[T](d: T)    = s"$d"
  def any2Option[T](d: T) = Some(d)

  val strInt    = plainInt.pipe(any2Str)
  val strDouble = plainDouble.pipe(any2Str)
  val strLong   = plainLong.pipe(any2Str)

  def str2Int(d: String)        = d.toInt
  def str2Long(d: String)       = d.toLong
  def str2BigDecimal(d: String) = BigDecimal(d)

  def number2sosu(n: Int)(targetnum: Int) = targetnum / Math.pow(10, n)

  def sosu_removeAfter(n: Int)(targetnum: Double) =
    // 다른방식으로 대체 가능
    Math.floor(targetnum * Math.pow(10, n)) / Math.pow(10, n).toDouble

  // sosu - all
  def numberMagic(sosu: Int)(targetNum: String) =
    targetNum match
      // sosu 1 / [올림,반올림,버림]
      case "12345.67891" =>
        Map(
          "올림"  -> "12345.7",
          "반올림" -> "12345.7",
          "버림"  -> "12345.6",
        )("올림")

      case _ => "11"

  type NumberGroup = Int | Double | Long | BigDecimal

  def set_MaximumPoint(n: Int) =
    val formatter = java.text.NumberFormat.getNumberInstance()
    formatter.setRoundingMode(RoundingMode.FLOOR)
    formatter.setMaximumFractionDigits(n)
    (d: NumberGroup) => formatter.format(d)

  def down_Points(n: Int)(targetnum: NumberGroup) =
    (BigDecimal(s"$targetnum") / Math.pow(10, n))
    // .pipe(set_MaximumPoint(n))

  def down_Points(n: Int)(targetnum: String) =
    (BigDecimal(targetnum) / Math.pow(10, n))
    // .pipe(set_MaximumPoint(n))

object TestoSample:
  import TestoLogic.*
  // 1234567891 => 12345.67891
  val sample1 = plainInt.pipe(number2sosu(5)).pipe(any2Str)
  val sample2 = plainInt.pipe(any2Option)

  // 12345.67891 -> 12345.6789 :: 소수점 4자리까지 자르기(내림)
  val sample4 = plainDouble.pipe(sosu_removeAfter(1)).pipe(any2Str)
  val sample3 = plainDouble.pipe(any2Str).pipe(numberMagic(1))

  val numberFixed =
    plainBigDecimal
      .pipe(down_Points(10))
      .pipe(set_MaximumPoint(18))

  // Case1. Some[Num?] => Some[Num] => Num => Num.0
  // Case2. Num? => Num => Num.0

  // function numberFixer1              ::  Num => Num.0
  // function numberFixer2              ::  Num? => Num
  // function numberFixer3              ::  Some[Num?] => Some[Num]
