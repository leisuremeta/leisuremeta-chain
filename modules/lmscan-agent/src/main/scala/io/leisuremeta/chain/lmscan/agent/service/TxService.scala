// package io.leisuremeta.chain.lmscan.agent.service

// import io.leisuremeta.chain.lmscan.agent.model.id
// import io.leisuremeta.chain.lmscan.agent.entity.Tx
// import cats.effect.kernel.Async
// import cats.data.EitherT
// import io.leisuremeta.chain.lmscan.agent.repository.CommonQuery

// object TxService:
//   def insert[F[_]: Async, T <: id](
//       tx: T,
//   ): EitherT[F, String, Long] =
//     CommonRepository.insert(tx)

    