// package io.leisuremeta.chain.lmscan.backend.service

// import cats.effect.kernel.Async
// import cats.data.EitherT

// import io.leisuremeta.chain.lmscan.common.model.PageNavigation
// import io.leisuremeta.chain.lmscan.common.model.PageResponse
// import io.leisuremeta.chain.lmscan.common.model.{BlockDetail, BlockInfo}
// import io.leisuremeta.chain.lmscan.backend.entity.Block
// import io.leisuremeta.chain.lmscan.backend.repository.BlockRepository

// object BlockService:
//   def getPage[F[_]: Async](
//       pageNavInfo: PageNavigation,
//   ): EitherT[F, String, PageResponse[BlockInfo]] =
//     for
//       page <- BlockRepository.getPage(pageNavInfo)
//       blockInfos = page.payload.map { block =>
//         BlockInfo(
//           Some(block.number),
//           Some(block.hash),
//           Some(block.txCount),
//           Some(block.eventTime),
//         )
//       }
//     yield PageResponse(page.totalCount, page.totalPages, blockInfos)

//   def get[F[_]: Async](
//       hash: String,
//   ): EitherT[F, String, Option[Block]] =
//     BlockRepository.get(hash)

//   def getByNumber[F[_]: Async](
//       number: Long,
//   ): EitherT[F, String, Option[Block]] =
//     BlockRepository.getByNumber(number)

//   def getDetail[F[_]: Async](
//       hash: String,
//   ): EitherT[F, String, Option[BlockDetail]] =
//     for
//       block <- get(hash)
//       txPage <- TransactionService.getPageByBlock(
//         hash,
//         new PageNavigation(0, 10),
//       )

//       blockInfo = block.map { bl =>
//         BlockDetail(
//           Some(bl.hash),
//           Some(bl.parentHash),
//           Some(bl.number),
//           Some(bl.eventTime),
//           Some(bl.txCount),
//           Some(txPage.payload),
//         )
//       }
//     yield blockInfo
