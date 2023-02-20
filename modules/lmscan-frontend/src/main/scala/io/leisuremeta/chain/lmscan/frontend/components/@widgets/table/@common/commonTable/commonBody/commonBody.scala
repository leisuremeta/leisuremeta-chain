package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
// package io.leisuremeta.chain.lmscan.frontend
// import Dom.{_hidden, isEqGet}
import ValidOutputData.*
import Dom.*

object Body:
  def block = (payload: List[Block]) =>
    payload
      .map(each =>
        div(`class` := "row table-body")(
          gen.cell(
            Cell.BLOCK_NUMBER(each.hash, each.number),
            Cell.AGE(each.createdAt),
            Cell.BLOCK_HASH(each.hash),
            Cell.PlainInt(each.txCount),
          ),
        ),
      )

  def nft = (payload: List[NftActivities]) =>
    payload
      .map(each =>
        val from = getOptionValue(each.fromAddr, "-").toString()
        val to   = getOptionValue(each.toAddr, "-").toString()

        div(
          `class` := "row table-body",
        )(
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.TransactionDetail(
                    getOptionValue(each.txHash, "-").toString(),
                  ),
                ),
              ),
            )(
              getOptionValue(each.txHash, "-")
                .toString(),
                // .take(10) + "...",
            ),
          ),
          div(`class` := "cell")(
            span()(
              yyyy_mm_dd_time(
                getOptionValue(each.createdAt, 0).asInstanceOf[Int],
              ),
            ),
          ),
          div(`class` := "cell")(
            span()(getOptionValue(each.action, "-").toString()),
          ),
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.AccountDetail(
                    getOptionValue(
                      each.fromAddr,
                      "-",
                    )
                      .toString(),
                  ),
                ),
              ),
            )(
              from.length match
                case 40 =>
                  from
                    .take(10) + "..."
                case _ =>
                  from.toString() match
                    case "playnomm" =>
                      "010cd45939f064fd82403754bada713e5a9563a1".take(
                        10,
                      ) + "..."
                    case "eth-gateway" =>
                      "ca79f6fb199218fa681b8f441fefaac2e9a3ead3".take(
                        10,
                      ) + "..."
                    case _ =>
                      from,
            ),
          ),
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.AccountDetail(
                    getOptionValue(
                      each.toAddr,
                      "-",
                    )
                      .toString(),
                  ),
                ),
              ),
            )(
              to.length match
                case 40 =>
                  to
                    .take(10) + "..."
                case _ =>
                  to.toString() match
                    case "playnomm" =>
                      "010cd45939f064fd82403754bada713e5a9563a1".take(
                        10,
                      ) + "..."
                    case "eth-gateway" =>
                      "ca79f6fb199218fa681b8f441fefaac2e9a3ead3".take(
                        10,
                      ) + "..."
                    case _ =>
                      to,
            ),
          ),
        ),
      )
