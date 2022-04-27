package org.leisuremeta.lmchain.core
package node
package service
package interpreter

import java.time.Instant

import cats.data.{EitherT, StateT}
import cats.effect.Concurrent
import cats.effect.concurrent.{Ref, Semaphore}
import cats.implicits._

import crypto.Hash.ops._
import crypto.Signature
import model.{Block, Signed}
import repository.{BlockRepository, StateRepository, TransactionRepository}
import service.{BlockService, StateService}

//import LocalGossipServiceInterpreter._

class LocalGossipServiceInterpreter[F[_]
  : Concurrent: StateRepository.Name: StateRepository.Token: StateRepository.Balance: TransactionRepository: BlockRepository](
    localGossipRef: Ref[F, GossipDomain.LocalGossip],
    lock: Semaphore[F],
    params: GossipDomain.GossipParams,
) extends LocalGossipService[F] {

  implicit val us: GossipDomain.UpdateState[F] =
    StateService.updateStateWithTx[F]
  override def get: F[GossipDomain.LocalGossip] = localGossipRef.get

  override def onNewTx(tx: Signed.Tx): EitherT[F, String, Unit] = {
    scribe.debug(s"===> onNewTx: $tx, $params")
    withLocalGossip {
      StateT.modifyF { (localGossip: GossipDomain.LocalGossip) =>
        GossipDomain.onNewTx[F](localGossip, tx)
      }
    }
  }

  override def generateNewBlockSuggestion(
      currentTime: Instant
  ): EitherT[F, String, Block.BlockHash] = {
    scribe.debug(s"===> generateNewBlockSuggestion: $currentTime")
    withLocalGossip {
      for {
        (blockHash, block) <- StateT.inspectF {
          (localGossip: GossipDomain.LocalGossip) =>
            GossipDomain
              .generateNewBlockSuggestion[F](localGossip, currentTime, params)
        }
        _ <- onNewBlockSuggestionProgram(block)
        _ <- tryFinalizeProgram(blockHash)
      } yield blockHash
    }
  }

  override def onNewBlockSuggestion(block: Block): EitherT[F, String, Unit] = {
    scribe.debug(s"===> onNewBlockSuggestion: $block")
    withLocalGossip {
      for {
        sigOption <- onNewBlockSuggestionProgram(block)
        _ <- sigOption.traverse { sig =>
          onNewBlockVoteProgram(block.toHash, params.localNodeIndex, sig)
        }
        _ <- tryFinalizeProgram(block.toHash)
      } yield ()
    }
  }

  override def onNewBlockVote(
      blockHash: Block.BlockHash,
      nodeNo: Int,
      sig: Signature,
  ): EitherT[F, String, Unit] = {
    scribe.debug(s"===> onNewBlockVote: $blockHash, $nodeNo, $sig")
    withLocalGossip {
      for {
        _ <- onNewBlockVoteProgram(blockHash, nodeNo, sig)
        _ <- tryFinalizeProgram(blockHash)
      } yield ()
    }
  }

  override def setBestConfirmedBlock(
      blockHash: Block.BlockHash,
      block: Block,
  ): EitherT[F, String, Unit] = {
    scribe.debug(s"===> setBestBlock: $block")
    withLocalGossip {
      StateT.modify(_.copy(bestConfirmed = (blockHash, block)))
    }
  }

  private def withLocalGossip[A](
      program: StateT[EitherT[F, String, *], GossipDomain.LocalGossip, A]
  ): EitherT[F, String, A] =
    EitherT(for {
      _           <- lock.acquire
      localGossip <- localGossipRef.get
      result      <- program.run(localGossip).value
      _ <- result match {
        case Right((localGossip1, _)) =>
          localGossipRef.set(localGossip1)
        case Left(_) =>
          Concurrent[F].pure(())
      }
      _ <- lock.release
    } yield result.map(_._2))

  private def onNewBlockSuggestionProgram(
      block: Block
  ): StateT[EitherT[F, String, *], GossipDomain.LocalGossip, Option[
    Signature
  ]] = {
    scribe.debug(s"===> onNewBlockSuggestionProgram: $block")
    StateT { (localGossip: GossipDomain.LocalGossip) =>
      GossipDomain.onNewBlockSuggestion[F](localGossip, block, params)
    }
  }

  private def onNewBlockVoteProgram(
      blockHash: Block.BlockHash,
      nodeNo: Int,
      signature: Signature,
  ): StateT[EitherT[F, String, *], GossipDomain.LocalGossip, Unit] = {
    scribe.debug(s"===> onNewBlockVoteProgram: $nodeNo -> $blockHash")
    StateT.modifyF { (localGossip: GossipDomain.LocalGossip) =>
      EitherT.fromEither[F](
        GossipDomain.onNewBlockVote(
          localGossip,
          blockHash,
          nodeNo,
          signature,
          params,
        )
      )
    }
  }

  private def tryFinalizeProgram(
      blockHash: Block.BlockHash
  ): StateT[EitherT[F, String, *], GossipDomain.LocalGossip, List[
    (Block.BlockHash, Block)
  ]] = {
    scribe.debug(s"===> tryFinalizeProgram: $blockHash")
    StateT { (localGossip: GossipDomain.LocalGossip) =>
      (for {
        (localGossip1, list) <- EitherT.fromEither[F](
          GossipDomain
            .tryFinalizeBlockWithBlockHash(localGossip, blockHash, params)
        )
        _ <- list.traverse { case (blockHash @ _, block) =>
          val txs: Map[Signed.TxHash, Signed.Tx] =
            block.transactionHashes.map { (txHash) =>
              (txHash, localGossip.newTxs(txHash))
            }.toMap
          BlockService.saveBlockWithState[F](block, txs)
        }
      } yield (localGossip1, list)).recover { (msg: String) =>
        scribe.debug(s"Fail to finalize: $msg")
        (localGossip, List.empty)
      }
    }
  }
}

object LocalGossipServiceInterpreter {
  def build[F[_]
    : Concurrent: StateRepository.Name: StateRepository.Token: StateRepository.Balance: TransactionRepository: BlockRepository](
      bestConfirmedBlock: Block,
      params: GossipDomain.GossipParams,
  ): F[LocalGossipService[F]] = for {
    localGossipLock <- Ref.of[F, GossipDomain.LocalGossip](
      GossipDomain.LocalGossip.empty(bestConfirmedBlock)
    )
    lock <- Semaphore[F](1)
  } yield new LocalGossipServiceInterpreter[F](localGossipLock, lock, params)
}
