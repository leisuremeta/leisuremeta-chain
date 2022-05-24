package io.leisuremeta.chain
package node
package service
package interpreter

import java.time.Instant

import cats.data.{EitherT, StateT}
import cats.effect.{Async, Concurrent, Deferred, Ref}
import cats.effect.std.Semaphore
import cats.implicits.*

import api.model.{Block, Signed}
import lib.crypto.Hash.ops.*
import lib.crypto.Signature
import repository.{BlockRepository, StateRepository, TransactionRepository}
import service.{BlockService, StateService}

//import LocalGossipServiceInterpreter._

class LocalGossipServiceInterpreter[F[_]
  : Async: Concurrent: StateRepository.AccountState.Name: StateRepository.AccountState.Key: TransactionRepository: BlockRepository](
    localGossipRef: Ref[F, GossipDomain.LocalGossip],
    lock: Semaphore[F],
    params: GossipDomain.GossipParams,
    txResponseRef: Ref[F, Map[Signed.TxHash, Either[Throwable, Either[String, Signed.TxHash]] => Unit]],
) extends LocalGossipService[F]:

  given us: GossipDomain.UpdateState[F] = StateService.updateStateWithTx[F]
  override def get: F[GossipDomain.LocalGossip] = localGossipRef.get

  override def onNewTx(tx: Signed.Tx): EitherT[F, String, Signed.TxHash] =
    scribe.info(s"===> onNewTx: $tx, $params")
    withLocalGossip {
      StateT.modifyF { (localGossip: GossipDomain.LocalGossip) =>
        GossipDomain.onNewTx[F](localGossip, tx).map { localGossip1 =>
          scribe.info(s"===> after onNewTx: ${localGossip1}")
          scribe.info(s"===> new txs: ${localGossip1.newTxs}")
          localGossip1
        }
      }
    } *> EitherT {
      scribe.info(s"===> updating response map")
      for
        result <- Async[F].async[Either[String, Signed.TxHash]]{ cb =>
          txResponseRef.update { map =>
            map + (tx.toHash -> cb)
          }.map(_ => None)
        }
      yield
        scribe.info(s"===> onNewTx result: $result")
        result
    }

  override def generateNewBlockSuggestion(
      currentTime: Instant,
  ): EitherT[F, String, Block.BlockHash] =
    scribe.debug(s"===> generateNewBlockSuggestion: $currentTime")
    withLocalGossip {
      for
        blockHashAndBlock <- StateT.inspectF {
          (localGossip: GossipDomain.LocalGossip) =>
            GossipDomain
              .generateNewBlockSuggestion[F](localGossip, currentTime, params)
        }
        (blockHash, block) = blockHashAndBlock
        _ <- onNewBlockSuggestionProgram(block)
        _ <- tryFinalizeProgram(blockHash)
      yield blockHash
    }

  override def onNewBlockSuggestion(block: Block): EitherT[F, String, Unit] =
    scribe.debug(s"===> onNewBlockSuggestion: $block")
    withLocalGossip {
      for
        sigOption <- onNewBlockSuggestionProgram(block)
        _ <- sigOption.traverse { sig =>
          onNewBlockVoteProgram(block.toHash, params.localNodeIndex, sig)
        }
        _ <- tryFinalizeProgram(block.toHash)
      yield ()
    }

  override def onNewBlockVote(
      blockHash: Block.BlockHash,
      nodeNo: Int,
      sig: Signature,
  ): EitherT[F, String, Unit] =
    scribe.debug(s"===> onNewBlockVote: $blockHash, $nodeNo, $sig")
    withLocalGossip {
      for
        _ <- onNewBlockVoteProgram(blockHash, nodeNo, sig)
        _ <- tryFinalizeProgram(blockHash)
      yield ()
    }

  override def setBestConfirmedBlock(
      blockHash: Block.BlockHash,
      block: Block,
  ): EitherT[F, String, Unit] =
    scribe.debug(s"===> setBestBlock: $block")
    withLocalGossip {
      StateT.modify(_.copy(bestConfirmed = (blockHash, block)))
    }

  private def withLocalGossip[A](
      program: StateT[EitherT[F, String, *], GossipDomain.LocalGossip, A],
  ): EitherT[F, String, A] = EitherT{
    for
      _           <- lock.acquire
      localGossip <- localGossipRef.get
      _ <- Concurrent[F].pure(scribe.info(s"===> start withLocalGossip: $localGossip"))
      result      <- program.run(localGossip).value
      _ <- result match
        case Right((localGossip1, _)) =>
          scribe.debug(s"===> Local Gossip Right Result: $localGossip1")
          localGossipRef.set(localGossip1)
        case Left(msg) =>
          scribe.debug(s"===> Local Gossip Left Result: $msg")
          Concurrent[F].pure(())
      _ <- lock.release
    yield
      result.map(_._2)
  }

  private def onNewBlockSuggestionProgram(
      block: Block,
  ): StateT[EitherT[F, String, *], GossipDomain.LocalGossip, Option[
    Signature,
  ]] =
    scribe.debug(s"===> onNewBlockSuggestionProgram: $block")
    StateT { (localGossip: GossipDomain.LocalGossip) =>
      GossipDomain.onNewBlockSuggestion[F](localGossip, block, params)
    }

  private def onNewBlockVoteProgram(
      blockHash: Block.BlockHash,
      nodeNo: Int,
      signature: Signature,
  ): StateT[EitherT[F, String, *], GossipDomain.LocalGossip, Unit] =
    scribe.debug(s"===> onNewBlockVoteProgram: $nodeNo -> $blockHash")
    StateT.modifyF { (localGossip: GossipDomain.LocalGossip) =>
      EitherT.fromEither[F](
        GossipDomain.onNewBlockVote(
          localGossip,
          blockHash,
          nodeNo,
          signature,
          params,
        ),
      )
    }

  private def tryFinalizeProgram(
      blockHash: Block.BlockHash,
  ): StateT[EitherT[F, String, *], GossipDomain.LocalGossip, List[
    (Block.BlockHash, Block),
  ]] =
    scribe.debug(s"===> tryFinalizeProgram: $blockHash")
    StateT { (localGossip: GossipDomain.LocalGossip) =>
      (for
        localGossip1AndList <- EitherT.fromEither[F](
          GossipDomain
            .tryFinalizeBlockWithBlockHash(localGossip, blockHash, params),
        )
        localGossip1 = localGossip1AndList._1
        GossipDomain.BlockFinalizationResult(blockList, removedTxHashSet) =
          localGossip1AndList._2
        _ <- blockList.traverse { case (blockHash @ _, block) =>
          val txs: Map[Signed.TxHash, Signed.Tx] =
            block.transactionHashes.map { (txHash) =>
              (txHash, localGossip.newTxs(txHash))
            }.toMap  
          BlockService.saveBlockWithState[F](block, txs) *>
            block.transactionHashes.toVector.traverse { txHash =>
              EitherT.right[String](txResponseRef.update{ map =>
                map(txHash)(Right(Right(txHash)))
                map.removed(txHash)
              })
            }
        }
        _ <- removedTxHashSet.toList.traverse{ txHash =>
          EitherT.right[String](txResponseRef.update{ map =>
            map(txHash)(Right(Left(s"tx $txHash is invalidated by another block")))
            map.removed(txHash)
          })
        }
      yield (localGossip1, blockList)).recover { (msg: String) =>
        scribe.info(s"Fail to finalize: $msg")
        (localGossip, List.empty)
      }
    }

object LocalGossipServiceInterpreter:
  def build[F[_]
    : Async: Concurrent: StateRepository.AccountState.Name: StateRepository.AccountState.Key: TransactionRepository: BlockRepository](
      bestConfirmedBlock: Block,
      params: GossipDomain.GossipParams,
  ): F[LocalGossipService[F]] = for
    localGossipLock <- Ref.of[F, GossipDomain.LocalGossip](
      GossipDomain.LocalGossip.empty(bestConfirmedBlock),
    )
    lock <- Semaphore[F](1)
    txResponseRef <- Ref.of[F, Map[Signed.TxHash, Either[Throwable, Either[String, Signed.TxHash]] => Unit]](
      Map.empty,
    )
  yield
    scribe.info(s"===> building LocalGossipServiceInterpreter")
    new LocalGossipServiceInterpreter[F](
      localGossipLock,
      lock,
      params,
      txResponseRef,
    )
