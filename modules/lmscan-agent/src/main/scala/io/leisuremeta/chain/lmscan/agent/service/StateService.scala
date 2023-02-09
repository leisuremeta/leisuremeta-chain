package io.leisuremeta.chain.lmscan.agent.service

import cats.effect.kernel.Async
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.agent.entity.{BlockStateEntity, TxStateEntity}
import io.leisuremeta.chain.lmscan.agent.repository.StateRepository

object StateService:
  def getBlockStatesByNotBuildedOrderByEventTimeAsc[F[_]: Async]: EitherT[F, String, Seq[BlockStateEntity]] =
    StateRepository.getBlockStatesByNotBuildedOrderByEventTimeAsc

  def getTxStatesByBlockOrderByEventTimeAsc[F[_]: Async](blockHash: String): EitherT[F, String, Seq[TxStateEntity]] =
    StateRepository.getTxStatesByBlockOrderByEventTimeAsc(blockHash)


