package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import V.*
import io.leisuremeta.chain.lmscan.common.model

object TxDetailTableINOUT:

  val input = (data: List[String]) =>
    data.zipWithIndex.map { ((input, i) => genInput(input, i + 1)) }(0)

  val output = (data: List[String]) =>
    data.zipWithIndex.map { ((input, i) => genOutput(input, i + 1)) }(0)

  val genInput = (data: String, i: Any) =>
    List(
      div(`class` := "cell type-detail-body")(i.toString()),
      div(`class` := "cell type-3 type-detail-body")(
        span(
          // onClick(
          //   PageMsg.PreUpdate(
          //     TxDetail(
          //       name = Transactions().name,
          //       url = s"txDetail/${plainStr(Some(data))}",
          //       pubs = List(PubCase.TxDetailPub(hash = plainStr(Some(data)))),
          //     ),
          //   ),
          // ),
        )(data),
      ),
    )
  val genOutput = (data: String, i: Any) =>
    List(
      div(`class` := "cell type-detail-body")(i.toString()),
      div(`class` := "cell type-3 type-detail-body")(
        span(
          // onClick(
          //   PageMsg.PreUpdate(
          //     TxDetail(
          //       name = Transactions().name,
          //       url = s"txDetail/${plainStr(Some(data))}",
          //       pubs = List(PubCase.TxDetailPub(hash = plainStr(Some(data)))),
          //     ),
          //   ),
          // ),
        )(data),
      ),
    )
