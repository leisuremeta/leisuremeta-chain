package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html._
import tyrian.SVG._
import tyrian.*
import io.leisuremeta.chain.lmscan.common.model._

object BoardView:
  def f(v: Long) = f"$v%,d" 
  def f(v: BigDecimal) = f"$v%,.0f" 
  val x = 400
  val y = 100

  def pricePan(summary: SummaryBoard, v: SummaryChart) = 
    div(cls := "board-comp chart")(
      span("Price"),
      summary.today.lmPrice match
        case None => LoaderView.view
        case Some(v) => span("$" + v.toString.take(6))
      , 
      (summary.today.lmPrice, summary.yesterday.lmPrice) match
        case (Some(v), Some(w)) => 
          val diff = (v - w) / w * 100
          div(
            span("24H"),
            span(cls := s"${if diff >= 0 then "pos" else "neg"}")(f"$diff%3.2f %%")
          )
        case (_, _) => div()
      ,
      makeChart(v.list.map(vv => vv.lmPrice.getOrElse(0.0)).reverse.take(144).toList, x, y),
    )

  def txPan(summary: SummaryBoard, v: SummaryChart) =
    div(cls := "board-comp chart")(
      span("Total Transaction"),
      span(f(summary.today.totalTxSize.getOrElse(0L))),
      drawDiff(summary.today.totalTxSize, summary.yesterday.totalTxSize, v => v.toString),
      makeChart(v.list.map(vv => vv.totalTxSize.map(_.toDouble).getOrElse(0.0)).reverse.toList, x, y),
    )

  def balPan(summary: SummaryBoard, v: SummaryChart) =
    def f(v: BigDecimal) = f"${v / Math.pow(10, 18)}%,.0f"
    div(cls := "board-comp")(
      span("Total Balance in LMC"),
      span(f(summary.today.totalBalance.getOrElse(BigDecimal(0)))) ,
      drawDiff(summary.today.totalBalance, summary.yesterday.totalBalance, f),
      makeChart(v.list.map(vv => vv.totalBalance.getOrElse(BigDecimal(0)).toDouble).reverse.take(144).toList, x, y),
    )
  
  def drawDiff[T](oT: Option[T], oY: Option[T], f: T => String = (v: T) => v.toString)(using nu: Numeric[T]) = (oT, oY) match 
    case (Some(t), Some(y)) => 
      val diff = nu.minus(t, y)
      div(
        span("24H"),
        span(cls := s"${if nu.gteq(t, y) then "pos" else "neg"}")(f(diff))
      )
    case (Some(v), _) => div(span("24H"), span(f(v)))
    case (_, Some(v)) => div(span("24H"), span(f(v)))
    case _ => div(span("24H"))

  def accPan(summary: SummaryBoard, v: SummaryChart) =
    div(cls := "board-comp")(
      span("Total Accounts"),
      span(f(summary.today.totalAccounts.getOrElse(0L))),
      drawDiff(summary.today.totalAccounts, summary.yesterday.totalAccounts),
      makeChart(v.list.map(vv => vv.totalAccounts.getOrElse(0L).toDouble).reverse.take(144).toList, x, y),
    )

  def normalize(xs: List[Double]): List[Double] =
    if xs.isEmpty then return xs
    val max = xs.max
    val min = xs.min
    val d = max - min
    if d == 0 then xs.map(_ => 0.5)
    else xs.map(x => (x - min) / d)

  def makeLine(ys: List[Double], x: Long, h: Long): String =
    val dx = x / (ys.size - 1).toDouble
    normalize(ys).zipWithIndex
    .map:
      case (y, i) if i == 0 => s"M0,${h - y * h} L${i * dx},${h - y * h}"
      case (y, i) => s"L${i * dx},${h - y * h}"
    .mkString("")

  def makeArea(ys: List[Double], x: Long, h: Long): String =
    val dx = x / (ys.size - 1).toDouble
    normalize(ys).zipWithIndex
    .map:
      case (y, i) if i == 0 => s"0,${h} 0,${h - y * h} ${i * dx},${h - y * h}"
      case (y, i) => s" ${i * dx},${h - y * h}"
    .mkString("", "", s" ${x},${h}")

  def makeChart(ys: List[Double], x: Long, h: Long): Html[Msg] =
    svg(viewBox := s"0, 0, ${x}, ${h}")(
      path(d := makeLine(ys, x, h)),
      polyline(points := makeArea(ys, x, h))
    )

  def marketCap(summary: SummaryBoard, v: SummaryChart) =
    div(cls := "board-comp")(
      span("Market cap"),
      span("$" + f(summary.today.marketCap.getOrElse(BigDecimal(0)))),
      drawDiff(summary.today.marketCap, summary.yesterday.marketCap, f),
      makeChart(v.list.map(vv => vv.marketCap.getOrElse(BigDecimal(0)).toDouble).reverse.take(144).toList, x, y),
    )
  def cirSupply(summary: SummaryBoard, v: SummaryChart) =
    def f(v: BigDecimal) = f"$v%,.0f"
    div(cls := "board-comp")(
      span("Circulation Supply"),
      span(f(summary.today.cirSupply.getOrElse(BigDecimal(0))) + " LM"), 
      drawDiff(summary.today.cirSupply, summary.yesterday.cirSupply, f),
      makeChart(v.list.map(vv => vv.cirSupply.getOrElse(BigDecimal(0)).toDouble).reverse.take(144).toList, x, y),
    )
  def totalBlock(summary: SummaryBoard, v: SummaryChart) =
    div(cls := "board-comp")(
      span("Total Blocks"),
      span(f(summary.today.blockNumber.getOrElse(0L))),
      drawDiff(summary.today.blockNumber, summary.yesterday.blockNumber, f),
      makeChart(v.list.map(vv => vv.blockNumber.getOrElse(0L).toDouble).reverse.take(144).toList, x, y),
    )
  def totalNft(summary: SummaryBoard, v: SummaryChart) =
    div(cls := "board-comp")(
      span("Total NFTs"),
      span(f(summary.today.totalNft.getOrElse(0L))), 
      drawDiff(summary.today.totalNft, summary.yesterday.totalNft, f),
      makeChart(v.list.map(vv => vv.totalNft.getOrElse(0L).toDouble).reverse.take(144).toList, x, y),
    )

  def view(model: BaseModel): Html[Msg] =
    (model.summary, model.chartData) match
      case (Some(summary), Some(v)) =>
        div(cls := "board-area")(
          pricePan(summary, v),
          marketCap(summary, v),
          cirSupply(summary, v),
          balPan(summary, v), 
          txPan(summary, v),
          accPan(summary, v), 
          totalBlock(summary, v),
          totalNft(summary, v),
        )
      case _ => div(cls := "board-area")(LoaderView.view)
