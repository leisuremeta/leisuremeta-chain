package io.leisuremeta.chain.lmscan.agent
package service

import doobie.util.transactor.Transactor
import doobie.*
import cats.*
import cats.effect.*

trait RemoteStoreApp[F[_]: MonadCancelThrow]:
  val blcRepo: BlockRepository[F]
  val txRepo: TxRepository[F]
  val summary: SummaryRepository[F]
  val nftRepo: NftRepository[F]
  val balRepo: BalanceRepository[F]
  val accRepo: AccountRepository[F]
trait LocalStoreApp[F[_]: MonadCancelThrow]:
  val balRepo: LocalBalanceRepository[F]

object StoreService:
  def buildRemote[F[_]: MonadCancelThrow](xa: Transactor[F]) =
    new RemoteStoreApp[F]:
      val blcRepo: BlockRepository[F]   = BlockRepository.build(xa)
      val txRepo: TxRepository[F]       = TxRepository.build(xa)
      val summary: SummaryRepository[F] = SummaryRepository.build(xa)
      val nftRepo: NftRepository[F]     = NftRepository.build(xa)
      val balRepo: BalanceRepository[F] = BalanceRepository.build(xa)
      val accRepo: AccountRepository[F] = AccountRepository.build(xa)

  def buildLocal[F[_]: MonadCancelThrow](xa: Transactor[F]) =
    new LocalStoreApp[F]:
      val balRepo: LocalBalanceRepository[F] = LocalBalanceRepository.build(xa)
