package io.leisuremeta.chain
package node
package service

import cats.{Functor, Monad}
import cats.data.EitherT
import cats.effect.Clock
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*

import api.model.{Block, Signed, Transaction, TransactionWithResult}
import api.model.TransactionWithResult.ops.*
import api.model.api_model.TxInfo
import repository.{BlockRepository, StateRepository, TransactionRepository}

object TransactionService:
  def submit[F[_]
    : Monad: Clock: BlockRepository: LocalGossipService: StateRepository.AccountState](
      txs: Seq[Signed.Tx],
  ): EitherT[F, String, Seq[Signed.TxHash]] =
    scribe.info(
      s"LocalGossipService instance: ${summon[LocalGossipService[F]]}",
    )
    for
      txHashes    <- txs.traverse { tx => LocalGossipService[F].onNewTx(tx) }
      localGossip <- EitherT.right[String](LocalGossipService[F].get)
      newTxs = localGossip.newTxs.keySet
      currentTime <- EitherT.right[String](Clock[F].realTimeInstant)
      _ <- EitherT.rightT[F, String](
        scribe.info(s"current localGossip: $localGossip"),
      )
      _ <- EitherT.rightT[F, String](scribe.info(s"current new txs: $newTxs"))
      _ <- EitherT.rightT[F, String](scribe.info(s"current time: $currentTime"))
      result <- LocalGossipService[F].generateNewBlockSuggestion(currentTime)
      _      <- EitherT.rightT[F, String](scribe.info(s"block result: $result"))
    yield txHashes

  def index[F[_]: Monad: BlockRepository: TransactionRepository](
    blockHash: Block.BlockHash
  ): EitherT[F, Either[String, String], Set[TxInfo]] = for
    blockOption <- BlockRepository[F].get(blockHash).leftMap(e => Left(e.msg))
    block <- EitherT.fromOption[F](blockOption, Right(s"block not found: $blockHash"))
    txInfoSet <- block.transactionHashes.toList.traverse { (txHash) =>
      for
        txOption <- TransactionRepository[F].get(txHash.toResultHashValue).leftMap(e => Left(e.msg))
        tx <- EitherT.fromOption[F](txOption, Left(s"tx not found: $txHash in block $blockHash"))
      yield
        val txType: String = tx.signedTx.value match
          case tx: Transaction.AccountTx => "Account"
          case tx: Transaction.GroupTx => "Group"
          case tx: Transaction.TokenTx => "Token"
          case tx: Transaction.RewardTx => "Reward"
          
        TxInfo(
          txHash = txHash,
          createdAt = tx.signedTx.value.createdAt,
          account = tx.signedTx.sig.account,
          `type` = txType
        )
    }.map(_.toSet)
  yield txInfoSet

  def get[F[_]: Functor: TransactionRepository](
      txHash: Signed.TxHash,
  ): EitherT[F, String, Option[TransactionWithResult]] =
    TransactionRepository[F].get(txHash.toResultHashValue).leftMap(_.msg)
