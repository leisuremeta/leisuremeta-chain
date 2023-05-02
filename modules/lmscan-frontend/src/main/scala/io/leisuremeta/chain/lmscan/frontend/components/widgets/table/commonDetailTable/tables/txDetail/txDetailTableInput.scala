package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import V.*

object TxDetailTableInput:
  val input = (data: List[String]) =>
    data.zipWithIndex.map { ((input, i) => genInput(input, i + 1)) }

  val genInput = (data: String, i: Any) =>
    div(`class` := "row")(
      div(`class` := "cell type-detail-body")(i.toString()),
      div(`class` := "cell type-3 type-detail-body")(
        span(
          onClick(
            PageMsg.PreUpdate(
              PageCase.TxDetail(
                name = PageCase.Transactions().name,
                url = s"txDetail/${plainStr(Some(data))}",
                pubs = List(PubCase.TxDetailPub(hash = plainStr(Some(data)))),
              ),
            ),
          ),
        )(data),
      ),
    )

  def view(inputHashs: List[String]) = div(`class` := "x")(
    div(`class` := "type-TableDetail table-container ")(
      div(`class` := "table w-[100%]")(
        div(`class` := "row")(
          div(`class` := "cell type-detail-head")("Input"),
          div(`class` := "cell type-detail-body font-bold")(
            "Transaction Hash",
          ),
        ) :: input(inputHashs),
      ),
    ),
  )
