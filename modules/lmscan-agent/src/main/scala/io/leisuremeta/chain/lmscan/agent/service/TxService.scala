// package io.leisuremeta.chain.lmscan.agent.service

// import io.leisuremeta.chain.lmscan.agent.entity.Tx
// import cats.effect.kernel.Async
// import cats.data.EitherT
// import io.leisuremeta.chain.lmscan.agent.repository.TxRepository

// object TxService:
//   def insert[F[_]: Async](
//       tx: Tx,
//   ): EitherT[F, String, Long] =
//     TxRepository.insertWithoutTransaction(tx)
