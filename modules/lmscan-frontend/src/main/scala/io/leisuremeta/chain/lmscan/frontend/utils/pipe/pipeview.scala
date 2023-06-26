package io.leisuremeta.chain.lmscan.frontend
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.frontend.V.*
import io.leisuremeta.chain.lmscan.frontend.TestoLogic.*

object Pipe:

  def _Down18(value: Option[NumberGroup]) =
    value
      .pipe(getOptionValuePipe[NumberGroup](0))
      .pipe(down_Points(18))

  def _Down18(value: NumberGroup) =
    value
      .pipe(down_Points(18))

  def _Down18(value: String) =
    value
      .pipe(down_Points(18))

  def _valueParser(lmprice: Double, balance: Option[BigDecimal]) =
    (lmprice * balance.pipe(_Down18))

  def accountDetailPageBalance(value: Option[BigDecimal]) = value
    .pipe(_Down18)
    .pipe(set_MaximumPoint(4)) + " LM"

  def accountDetailPageValue(lmprice: Double, balance: Option[BigDecimal]) =
    "$ " + _valueParser(lmprice, balance)
      .pipe(set_MaximumPoint(4))

  def txDetailTableOutput(value: Option[String]) =
    BigDecimal(value.pipe(getOptionValuePipe[String]("0")))
      .pipe(_Down18)
      .pipe(set_MaximumPoint(4))
