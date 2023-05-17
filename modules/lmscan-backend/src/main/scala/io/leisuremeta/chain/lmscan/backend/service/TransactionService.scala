// package io.leisuremeta.chain.lmscan.backend.service

// // import io.leisuremeta.chain.lmscan.backend.entity.Tx
// import io.leisuremeta.chain.lmscan.common.model.TxInfo
// import io.leisuremeta.chain.lmscan.common.model.PageNavigation
// import io.leisuremeta.chain.lmscan.common.model.PageResponse
// import io.leisuremeta.chain.lmscan.common.model.{TxDetail, TransferHist, TxInfo}
// import io.leisuremeta.chain.lmscan.backend.repository.TransactionRepository
// import cats.Functor
// import cats.data.EitherT
// import cats.Monad
// import eu.timepit.refined.boolean.False
// import cats.effect.Async
// // import io.leisuremeta.ExploreApi
// import io.leisuremeta.chain.lmscan.common.ExploreApi
// import cats.implicits.catsSyntaxEitherId
// import cats.effect.IO
// import cats.effect.kernel.Async
// import io.leisuremeta.chain.lmscan.backend.repository.Dao
// import io.leisuremeta.chain.lmscan.common.model.dao.Tx

// object TransactionService:

//   def convertToInfo(txs: Seq[Tx]): Seq[TxInfo] =
//     txs.map { tx =>
//       val latestOutValOpt = tx.outputVals match
//         case Some(seq) =>
//           val x = seq.map(_.split("/"))
//           x.headOption.map(x =>
//             if x.isEmpty then ""
//             else x(1),
//           )
//         case None => None

//       TxInfo(
//         Some(tx.hash),
//         Some(tx.blockNumber),
//         Some(tx.eventTime),
//         Some(tx.txType),
//         Some(tx.tokenType),
//         Some(tx.fromAddr),
//         Some(tx.subType),
//         None,
//         latestOutValOpt,
//       )
//     }

//   def convertToInfoForAccount(txs: Seq[Tx], address: String): Seq[TxInfo] =
//     txs.map { tx =>
//       val latestOutValOpt = tx.outputVals match
//         // case Some(seq) => seq.map(_.split("/")).headOption.map(_(1))
//         case Some(seq) =>
//           val x = seq.map(_.split("/"))
//           x.headOption.map(x =>
//             if x.isEmpty then ""
//             else x(1),
//           )
//         case None => None

//       TxInfo(
//         Some(tx.hash),
//         Some(tx.blockNumber),
//         Some(tx.eventTime),
//         Some(tx.txType),
//         Some(tx.tokenType),
//         Some(tx.fromAddr),
//         Some(if tx.fromAddr == address then "Out" else "In"),
//         latestOutValOpt,
//       )
//     }
