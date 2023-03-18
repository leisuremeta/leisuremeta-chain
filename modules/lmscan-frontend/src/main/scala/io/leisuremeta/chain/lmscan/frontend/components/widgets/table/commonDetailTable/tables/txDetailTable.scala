// package io.leisuremeta.chain.lmscan.frontend

// import tyrian.Html.*
// import tyrian.*
// import _root_.io.circe.Decoder.state
// import Dom.*
// import V.*
// import java.math.RoundingMode
// import io.leisuremeta.chain.lmscan.common.model.TxDetail
// import io.leisuremeta.chain.lmscan.common.model.TransferHist

// object TxDetailTable:
//   val view = (model: Model) =>
//     val data: TxDetail = TxDetailParser
//       .decodeParser(model.txDetailData.get)
//       .getOrElse(new TxDetail)
//     genView(model, data)

//   val input = (data: List[String], isModal: Boolean) =>
//     isModal match
//       case true =>
//         data.zipWithIndex.map { ((input, i) => genInputForModal(input, i + 1)) }
//       case false =>
//         data.zipWithIndex.map { ((input, i) => genInput(input, i + 1)) }

//   val output = (data: List[TransferHist]) =>
//     data.zipWithIndex.map { case ((output), i) => genOutput(output, i + 1) }
//   val output_NFT = (data: List[TransferHist]) =>
//     data.zipWithIndex.map { case ((output), i) => genOutput_NFT(output, i + 1) }

//   val genInput = (data: String, i: Any) =>
//     div(`class` := "row")(
//       div(`class` := "cell type-detail-body")(i.toString()),
//       div(`class` := "cell type-3 type-detail-body")(
//         span(
//           onClick(
//             PageMsg.PreUpdate(
//               PageName.TransactionDetail(
//                 plainStr(Some(data)),
//               ),
//             ),
//           ),
//         )(data),
//       ),
//     )

//   val genInputForModal = (data: String, i: Any) =>
//     div(`class` := "row")(
//       div(`class` := "cell type-detail-body")(i.toString()),
//       div(`class` := "cell type-detail-body")(data),
//     )

//   val genOutput = (data: TransferHist, i: Any) =>
//     val formatter = java.text.NumberFormat.getNumberInstance()
//     formatter.setRoundingMode(RoundingMode.FLOOR)
//     formatter.setMaximumFractionDigits(18)

//     val value = getOptionValue(data.value, "0").toString().toDouble / Math
//       .pow(10, 18)
//       .toDouble

//     val formattedValue = formatter.format(value)

//     div(`class` := "row")(
//       div(`class` := "cell type-detail-head")(i.toString()),
//       div(`class` := "cell type-3 type-detail-body")(
//         span(
//           onClick(
//             PageMsg.PreUpdate(
//               PageName.AccountDetail(
//                 plainStr(data.toAddress),
//               ),
//             ),
//           ),
//         )(getOptionValue(data.toAddress, "-").toString()),
//       ),
//       div(`class` := "cell type-detail-body")(
//         formattedValue,
//       ),
//     )
//   val genOutput_NFT = (data: TransferHist, i: Any) =>
//     div(`class` := "row")(
//       div(`class` := "cell type-detail-head")(i.toString()),
//       div(`class` := "cell type-3 type-detail-body")(
//         span(
//           onClick(
//             PageMsg.PreUpdate(
//               PageName.AccountDetail(
//                 plainStr(data.toAddress),
//               ),
//             ),
//           ),
//         )(getOptionValue(data.toAddress, "-").toString()),
//       ),
//     )

//   val genView = (model: Model, data: TxDetail) =>
//     val transferHist = getOptionValue(data.transferHist, List())
//       .asInstanceOf[List[TransferHist]]
//     val inputHashs = getOptionValue(data.inputHashs, List())
//       .asInstanceOf[List[String]]

//     div(`class` := "y-start gap-10px w-[100%] ")(
//       div(`class` := "x")(
//         div(`class` := "type-TableDetail  table-container")(
//           div(`class` := "table w-[100%] ")(
//             div(`class` := "row")(
//               gen.cell(
//                 Cell.Head("Transaction Hash", "cell type-detail-head"),
//                 Cell.PlainStr(data.hash, "cell type-detail-body"),
//               ),
//             ),
//             div(`class` := "row")(
//               gen.cell(
//                 Cell.Head("Created At", "cell type-detail-head"),
//                 Cell.DATE(data.createdAt, "cell type-detail-body"),
//               ),
//             ),
//             div(`class` := "row")(
//               gen.cell(
//                 Cell.Head("Signer", "cell type-detail-head"),
//                 Cell.ACCOUNT_HASH_DETAIL(data.signer, "cell type-detail-body"),
//               ),
//             ),
//             div(`class` := "row")(
//               gen.cell(
//                 Cell.Head("Type", "cell type-detail-head"),
//                 Cell.PlainStr(data.txType, "cell type-detail-body"),
//               ),
//             ),
//             div(`class` := "row")(
//               gen.cell(
//                 Cell.Head("Token Type", "cell type-detail-head"),
//                 Cell.PlainStr(data.tokenType, "cell type-detail-body"),
//               ),
//             ),
//           ),
//         ),
//       ),
//       div(`class` := "x")(
//         div(`class` := "type-TableDetail table-container ")(
//           div(`class` := "table w-[100%]")(
//             div(`class` := "row")(
//               div(`class` := "cell type-detail-head")("Input"),
//               div(`class` := "cell type-detail-body font-bold")(
//                 "Transaction Hash",
//               ),
//               inputHashs.length > 5 match
//                 case true =>
//                   div(`class` := s"type-2 pt-16px")(
//                     span(
//                       `class` := s"${State.toggleTxDetailInput(model, ToggleMsg.ClickTxDetailInput, "_button")} ",
//                       onClick(ToggleMsg.ClickTxDetailInput),
//                     )("More"),
//                   )
//                 case false => div(),
//             )
//               :: input(inputHashs.slice(0, 5), false),
//           ),
//         ),
//       ),
//       div(`class` := "x")(
//         div(`class` := "type-TableDetail table-container")(
//           div(`class` := "table w-[100%]")(
//             div(`class` := "row")(
//               div(`class` := "cell type-detail-head")("Output"),
//               div(`class` := "cell type-detail-body font-bold")(
//                 "To",
//               ),
//               div(`class` := "cell type-detail-body font-bold")(
//                 s"${getOptionValue(data.tokenType, "-").toString() == "NFT" match
//                     case true  => "Token ID"
//                     case false => "Value"
//                   }",
//               ),
//             )
//               :: {
//                 getOptionValue(data.tokenType, "-").toString() match
//                   case "NFT" => output_NFT(transferHist)
//                   case _     => output(transferHist)
//               },
//           ),
//         ),
//       ),

//       // div(
//       //   `class` := s"type-2 pt-16px",
//       // )(
//       //   span(
//       //     `class` := s"${State.toggle(model, ToggleMsg.Click, "_button")} ",
//       //     onClick(ToggleMsg.Click),
//       //   )("More"),
//       // ),
//       // div(`class` := "pt-12px x-center")(
//       //   textarea(
//       //     `id`    := s"transaction-text-area",
//       //     `class` := s"${State.toggle(model, ToggleMsg.Click, "_textarea")}",
//       //   )(s"${TxDetailParser.txDetailEncoder(data)}"),
//       // ),
//       // div(
//       //   `class` := s"${State.toggleTxDetailInput(model, ToggleMsg.ClickTxDetailInput, "_table")}",
//       // )(
//       //   div(`class` := "type-TableDetail table-container txDetailModalTable")(
//       //     div(`class` := "table w-[100%]")(
//       //       div(`class` := "row")(
//       //         div(`class` := "cell type-detail-head")("Input"),
//       //         div(`class` := "cell type-detail-body font-bold")(
//       //           "Transaction Hash",
//       //         ),
//       //         div(`class` := s"type-2 pt-16px")(
//       //           span(onClick(ToggleMsg.ClickTxDetailInput))("Close"),
//       //         ),
//       //       )
//       //         :: input(inputHashs, true),
//       //     ),
//       //   ),
//       // ),
//     )
