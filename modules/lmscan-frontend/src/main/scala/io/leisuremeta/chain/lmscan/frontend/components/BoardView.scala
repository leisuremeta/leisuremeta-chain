package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.common.model._

object BoardView:
  val LM_Price = "LM PRICE"
  val Total_TxCount = "TOTAL TRANSACTIONS"
  val Transactions  = "TOTAL BALANCE"
  val Accounts = "TOTAL ACCOUNTS"
  def toggleClass(x: Boolean) = if(x) "pos" else "neg"

  def parseToNumber(strNum: BigDecimal) = f"${strNum / Math.pow(10, 18)}%,.3f"

  def addComma(numberString: Long) = f"${BigInt(numberString)}%,d"

  def drawDiff(diff: String, swt: Boolean) =
      div(
        `class` := "diff",
      )(
        span("24H"),
        span(`class` := s"${toggleClass(swt)}")(diff)
      )

  def pricePan(summary: SummaryBoard) = 
    List(drawPrice(summary.today.lmPrice), drawPriceDiff(summary.today.lmPrice, summary.yesterday.lmPrice))
    
  def drawPrice(opt: Option[Double]): Html[Msg] = opt match
    case None => LoaderView.view
    case Some(v) => drawContent(v.toString.take(6) + " USDT")

  def drawPriceDiff(optT: Option[Double], optY: Option[Double]): Html[Msg] = (optT, optY) match
    case (Some(v), Some(w)) => 
      val diff = (v - w) / w * 100
      drawDiff(f"$diff%+3.2f %%", diff >= 0)
    case (_, _) => div()

  def txPan(summary: SummaryBoard) =
    List(drawTx(summary.today.totalTxSize), drawTxDiff(summary.today.totalTxSize, summary.yesterday.totalTxSize))

  def drawTx(opt: Option[Long]): Html[Msg] = opt match
    case None => LoaderView.view
    case Some(v) => drawContent(addComma(v))

  def drawTxDiff(optT: Option[Long], optY: Option[Long]): Html[Msg] = (optT, optY) match
    case (Some(v), Some(w)) => 
      val diff = v - w 
      drawDiff(f"$diff%+d", diff >= 0)
    case (_, _) => div()

  def balPan(summary: SummaryBoard) =
    List(drawBal(summary.today.totalBalance), drawBalDiff(summary.today.totalBalance, summary.yesterday.totalBalance))

  def drawBal(opt: Option[BigDecimal]): Html[Msg] = opt match
    case None => LoaderView.view
    case Some(v) => drawContent(f"${v / Math.pow(10, 18)}%,.3f")

  def drawBalDiff(optT: Option[BigDecimal], optY: Option[BigDecimal]): Html[Msg] = (optT, optY) match
    case (Some(v), Some(w)) => 
      val diff = v - w 
      drawDiff(f"${diff / Math.pow(10, 18)}%+,.3f", diff >= 0)
    case (_, _) => div()

  def accPan(summary: SummaryBoard) =
    List(drawAcc(summary.today.totalAccounts), drawAccDiff(summary.today.totalAccounts, summary.yesterday.totalAccounts))

  def drawAcc(opt: Option[Long]): Html[Msg] = opt match
    case None => LoaderView.view
    case Some(v) => drawContent(addComma(v))

  def drawAccDiff(optT: Option[Long], optY: Option[Long]): Html[Msg] = (optT, optY) match
    case (Some(v), Some(w)) => 
      val diff = v - w 
      drawDiff(f"$diff%+d", diff >= 0)
    case (_, _) => div()

  def drawContent(s: String) = div(`class` := "color-white font-bold")(s)

  def drawBox(title: String, content: List[Html[Msg]], to: RouterMsg): Html[Msg] =
    div(
      `class` := "board-container xy-center position-relative",
      // onClick(to),
    )(
      div(
        `class` := "board-text y-center gap-10px",
      )(
        div(`class` := "font-16px color-white font-bold")(title) :: content,
      ),
    )

  def view(model: BaseModel): Html[Msg] =
    val summary = model.summary
    div(`class` := "board-area")(
      List(
        (LM_Price, pricePan(summary), RouterMsg.NavigateToUrl("https://coinmarketcap.com/currencies/leisuremeta/")),
        (Total_TxCount, txPan(summary), RouterMsg.NavigateTo(TotalTxChart)),
        (Transactions, balPan(summary), RouterMsg.NavigateTo(TotalBalChart)),
        (Accounts, accPan(summary), RouterMsg.NavigateTo(TotalAcChart)),
      ).map(drawBox)
    )
