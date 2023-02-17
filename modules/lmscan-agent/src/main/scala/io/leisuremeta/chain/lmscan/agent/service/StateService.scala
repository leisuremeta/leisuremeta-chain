package io.leisuremeta.chain.lmscan.agent.service

import cats.effect.kernel.Async
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.agent.entity.{BlockStateEntity, TxStateEntity}
import io.leisuremeta.chain.lmscan.agent.repository.StateRepository

object StateService:
  def getBlockStatesByNotBuildedOrderByNumberAsc[F[_]: Async]: EitherT[F, String, Seq[BlockStateEntity]] =
    StateRepository.getBlockStatesByNotBuildedOrderByNumberAsc

  def getBlockStateByNotBuildedOrderByNumberAsc[F[_]: Async]: EitherT[F, String, Option[BlockStateEntity]] =
    StateRepository.getBlockStateByNotBuildedOrderByNumberAsc

  def getBlockStateByNotBuildedOrderByNumberAscLimit[F[_]: Async](limit: Int): EitherT[F, String, Option[BlockStateEntity]] =
    StateRepository.getBlockStateByNotBuildedOrderByNumberAscLimit(limit)

  def getTxStatesByBlockOrderByEventTimeAsc[F[_]: Async](blockHash: String): EitherT[F, String, Seq[TxStateEntity]] =
    StateRepository.getTxStatesByBlockOrderByEventTimeAsc(blockHash)


