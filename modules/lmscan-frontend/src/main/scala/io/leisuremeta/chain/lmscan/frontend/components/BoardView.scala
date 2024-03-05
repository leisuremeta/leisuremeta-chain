package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html._
import tyrian.SVG._
import tyrian.*
import io.leisuremeta.chain.lmscan.common.model._

object BoardView:
  def f(v: Long) = f"$v%,d" 
  def f(v: BigDecimal) = f"$v%,.4f" 

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
      makeChart(v.list.map(vv => vv.lmPrice.getOrElse(0.0)).reverse.take(144).toList, 100, 200),
    )

  def txPan(summary: SummaryBoard, v: SummaryChart) =
    div(cls := "board-comp chart")(
      span("Total Transaction"),
      span(f(summary.today.totalTxSize.getOrElse(0L))),
      drawDiff(summary.today.totalTxSize, summary.yesterday.totalTxSize, v => v.toString),
      makeChart(v.list.map(vv => vv.totalTxSize.map(_.toDouble).getOrElse(0.0)).reverse.toList, 100, 200),
    )

  def balPan(summary: SummaryBoard) =
    def f(v: BigDecimal) = f"${v / Math.pow(10, 18)}%,.3f"
    div(cls := "board-comp")(
      span("Total Balance in LMC"),
      span(f(summary.today.totalBalance.getOrElse(BigDecimal(0)))) ,
      drawDiff(summary.today.totalBalance, summary.yesterday.totalBalance, f)
    )
  
  def drawDiff[T](oT: Option[T], oY: Option[T], f: T => String = (v: T) => v.toString)(using nu: Numeric[T]) = (oT, oY) match 
    case (Some(t), Some(y)) => 
      val diff = nu.minus(t, y)
      div(
        span("24H"),
        span(cls := s"${if nu.gteq(t, y) then "pos" else "neg"}")(f(diff))
      )
    case (Some(v), _) => div(span("24H"), span(v.toString))
    case (_, Some(v)) => div(span("24H"), span(v.toString))
    case _ => div(span("24H"))

  def accPan(summary: SummaryBoard) =
    div(cls := "board-comp")(
      span("Total Accounts"),
      span(f(summary.today.totalAccounts.getOrElse(0L))),
      drawDiff(summary.today.totalAccounts, summary.yesterday.totalAccounts)
    )

  def normalize(xs: List[Double]): List[Double] =
    val max = xs.max
    val min = xs.min
    val d = max - min
    xs.map(x => (x - min) / d)

  def makeLine(ys: List[Double], h: Long, x: Long): String =
    val dx = x / (ys.size - 1).toDouble
    normalize(ys).zipWithIndex
    .map:
      case (y, i) if i == 0 => s"M0,${h - y * h} L${i * dx},${h - y * h}"
      case (y, i) => s"L${i * dx},${h - y * h}"
    .mkString("")

  def makeArea(ys: List[Double], h: Long, x: Long): String =
    val dx = x / (ys.size - 1).toDouble
    normalize(ys).zipWithIndex
    .map:
      case (y, i) if i == 0 => s"0,${h} 0,${h - y * h} ${i * dx},${h - y * h}"
      case (y, i) => s" ${i * dx},${h - y * h}"
    .mkString("", "", s" ${x},${h}")

  def makeChart(ys: List[Double], h: Long, x: Long): Html[Msg] =
    svg(viewBox := s"0, 0, ${x}, ${h}")(
      path(d := makeLine(ys, h, x)),
      polyline(points := makeArea(ys, h, x))
    )

  def marketCap(summary: SummaryBoard) =
    div(cls := "board-comp")(
      span("Market cap"),
      span("$" + f(summary.today.marketCap.getOrElse(BigDecimal(0)))),
      drawDiff(summary.today.marketCap, summary.yesterday.marketCap, f),
    )
  def cirSupply(summary: SummaryBoard) =
    def f(v: BigDecimal) = f"$v%,.2f"
    div(cls := "board-comp")(
      span("Circulation Supply"),
      span(f(summary.today.cirSupply.getOrElse(BigDecimal(0))) + " LM"), 
      drawDiff(summary.today.cirSupply, summary.yesterday.cirSupply, f),
    )
  def totalBlock(summary: SummaryBoard) =
    div(cls := "board-comp")(
      span("Total Blocks"),
      span(f(summary.today.blockNumber.get)),
      drawDiff(summary.today.blockNumber, summary.yesterday.blockNumber, f),
    )
  def totalNft(summary: SummaryBoard) =
    div(cls := "board-comp")(
      span("Total NFTs"),
      span(f(summary.today.totalNft.getOrElse(0L))), 
      drawDiff(summary.today.totalNft, summary.yesterday.totalNft, f),
    )

  def view(model: BaseModel): Html[Msg] =
    (model.summary, model.chartData) match
      case (Some(summary), Some(v)) =>
        div(cls := "board-area")(
          pricePan(summary, v),
          marketCap(summary),
          cirSupply(summary),
          balPan(summary), 
          txPan(summary, v),
          accPan(summary), 
          totalBlock(summary),
          totalNft(summary),
        )
      case _ => div(cls := "board-area")(LoaderView.view)
