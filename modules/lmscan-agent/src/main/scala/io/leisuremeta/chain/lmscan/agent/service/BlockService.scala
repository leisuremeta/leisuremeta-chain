package io.leisuremeta.chain.lmscan.agent.service

import cats.data.EitherT
import cats.effect.kernel.Async
import io.leisuremeta.chain.lmscan.agent.repository.BlockRepository
import io.leisuremeta.chain.lmscan.agent.entity.{BlockSavedLog, BlockStateEntity, BlockEntity}
import cats.effect.kernel.Async

object BlockService:
  def get[F[_]: Async](
    hash: String,
  ): EitherT[F, String, Option[BlockEntity]] =
    BlockRepository.get(hash)

  def getLastSavedBlock[F[_]: Async]: 
    EitherT[F, String, Option[BlockSavedLog]] =
    BlockRepository.getLastSavedBlock

  def getLastBuildedBlock[F[_]: Async]:
    EitherT[F, String, Option[BlockStateEntity]] =
    BlockRepository.getLastBuildedBlock

  // def countBlockNumber[F[_]: Async]: EitherT[F, String, Option[Long]]
  //   BlockRepository.getL
