// package io.leisuremeta.chain.lmscan.backend.service

// import cats.effect.kernel.Async
// import cats.data.EitherT

// import io.leisuremeta.chain.lmscan.common.model.{
//   PageNavigation,
//   PageResponse,
//   AccountDetail,
// }

// object AccountService:
//   def get[F[_]: Async](
//       address: String,
//   ): EitherT[F, String, Option[AccountDetail]] =
//     val res = for
//       account <- AccountRepository.get(address)
//       txPage <- TransactionService.getPageByAccount(
//         address,
//         new PageNavigation(0, 20),
//       )
//     yield (account, txPage)

//     res.map { (accountOpt, page) =>
//       accountOpt match
//         case Some(x) =>
//           val detail = AccountDetail(
//             Some(x.address),
//             Some(x.balance),
//             Some(x.amount),
//             Some(page.payload),
//           )
//           Some(detail)
//         case None =>
//           scribe.info(s"there is no exist account of '$address'")
//           Option.empty
//     }
