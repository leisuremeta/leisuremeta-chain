package io.leisuremeta.chain
package node
package service

import cats.{Functor, Monad}
import cats.arrow.FunctionK
import cats.data.{EitherT, Kleisli, StateT}
import cats.effect.{Clock, Concurrent, Resource}
import cats.effect.std.Semaphore
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*

import scodec.bits.ByteVector

import api.model.{Block, Signed, StateRoot, Transaction, TransactionWithResult}
import api.model.Block.ops.*
import api.model.TransactionWithResult.ops.*
import api.model.api_model.TxInfo
import dapp.{PlayNommDApp, PlayNommDAppFailure, PlayNommState}
import lib.crypto.{Hash, KeyPair}
import lib.crypto.Hash.ops.*
import lib.crypto.Sign.ops.*
import lib.datatype.BigNat
import lib.merkle.{MerkleTrie, MerkleTrieNode, MerkleTrieState}
import lib.merkle.{
  GenericMerkleTrie,
  GenericMerkleTrieNode,
  GenericMerkleTrieState,
}
import lib.merkle.MerkleTrie.NodeStore
import lib.merkle.GenericMerkleTrie.{NodeStore as GenericNodeStore}
import repository.{
  BlockRepository,
  GenericStateRepository,
  StateRepository,
  TransactionRepository,
}

object TransactionService:
  def submit[F[_]
    : Concurrent: Clock: BlockRepository: TransactionRepository: StateRepository: PlayNommState](
      semaphore: Semaphore[F],
      txs: Seq[Signed.Tx],
      localKeyPair: KeyPair,
  ): EitherT[F, PlayNommDAppFailure, Seq[Hash.Value[TransactionWithResult]]] =
    Resource
      .make:
        EitherT
          .pure(semaphore.acquire)
          .map: _ =>
            scribe.info(s"Lock Acquired: $txs")
            ()
      .apply: _ =>
        EitherT.pure:
          scribe.info(s"Lock Released: $txs")
          semaphore.release
      .use: _ =>
        submit0[F](txs, localKeyPair)

  private def submit0[F[_]
    : Concurrent: Clock: BlockRepository: TransactionRepository: StateRepository: PlayNommState](
      txs: Seq[Signed.Tx],
      localKeyPair: KeyPair,
  ): EitherT[F, PlayNommDAppFailure, Seq[Hash.Value[TransactionWithResult]]] =
    for
      time0 <- EitherT.liftF(Clock[F].realTime)
      bestBlockHeaderOption <- BlockRepository[F].bestHeader.leftMap: e =>
        scribe.error(s"Best Header Error: $e")
        PlayNommDAppFailure.internal(s"Fail to get best header: ${e.msg}")
      bestBlockHeader <- EitherT.fromOption[F](
        bestBlockHeaderOption,
        PlayNommDAppFailure.internal("No best Header Available"),
      )
      baseState = MerkleTrieState.fromRootOption(bestBlockHeader.stateRoot.main)
      result <- txs
        .traverse(PlayNommDApp[F])
        .run(baseState)
      (finalState, txWithResults) = result
      txHashes                    = txWithResults.map(_.toHash)
      txState = txs
        .map(_.toHash)
        .sortBy(_.toUInt256Bytes.toBytes)
        .foldLeft(MerkleTrieState.empty): (state, txHash) =>
          given idNodeStore: NodeStore[cats.Id] = Kleisli.pure(None)
          MerkleTrie
            .put[cats.Id](
              txHash.toUInt256Bytes.toBytes.bits,
              ByteVector.empty,
            )
            .runS(state)
            .value
            .getOrElse(state)
      stateRoot1 = StateRoot(finalState.root)
      now <- EitherT.right(Clock[F].realTimeInstant)
      header = Block.Header(
        number = BigNat.add(bestBlockHeader.number, BigNat.One),
        parentHash = bestBlockHeader.toHash.toBlockHash,
        stateRoot = stateRoot1,
        transactionsRoot = txState.root,
        timestamp = now,
      )
      sig <- EitherT
        .fromEither(header.toHash.signBy(localKeyPair))
        .leftMap: msg =>
          scribe.error(s"Fail to sign header: $msg")
          PlayNommDAppFailure.internal(s"Fail to sign header: $msg")
      block = Block(
        header = header,
        transactionHashes = txHashes.toSet.map(_.toSignedTxHash),
        votes = Set(sig),
      )
      _ <- BlockRepository[F]
        .put(block)
        .leftMap: e =>
          scribe.error(s"Fail to put block: $e")
          PlayNommDAppFailure.internal(s"Fail to put block: ${e.msg}")
      _ <- StateRepository[F]
        .put(finalState)
        .leftMap: e =>
          scribe.error(s"Fail to put state: $e")
          PlayNommDAppFailure.internal(s"Fail to put state: ${e.msg}")
      _ <- txWithResults.traverse: txWithResult =>
        EitherT.liftF:
          TransactionRepository[F].put(txWithResult)
      time1 <- EitherT.right(Clock[F].realTime)
    yield
      scribe.info(s"total time consumed: ${time1 - time0}")
      txHashes

  def index[F[_]: Monad: BlockRepository: TransactionRepository](
      blockHash: Block.BlockHash,
  ): EitherT[F, Either[String, String], Set[TxInfo]] = for
    blockOption <- BlockRepository[F].get(blockHash).leftMap(e => Left(e.msg))
    block <- EitherT
      .fromOption[F](blockOption, Right(s"block not found: $blockHash"))
    txInfoSet <- block.transactionHashes.toList
      .traverse { (txHash) =>
        for
          txOption <- TransactionRepository[F]
            .get(txHash.toResultHashValue)
            .leftMap(e => Left(e.msg))
          tx <- EitherT.fromOption[F](
            txOption,
            Left(s"tx not found: $txHash in block $blockHash"),
          )
        yield
          val txType: String = tx.signedTx.value match
            case tx: Transaction.AccountTx => "Account"
            case tx: Transaction.GroupTx   => "Group"
            case tx: Transaction.TokenTx   => "Token"
            case tx: Transaction.RewardTx  => "Reward"
            case tx: Transaction.AgendaTx  => "Agenda"

          TxInfo(
            txHash = txHash,
            createdAt = tx.signedTx.value.createdAt,
            account = tx.signedTx.sig.account,
            `type` = txType,
          )
      }
      .map(_.toSet)
  yield txInfoSet

  def get[F[_]: Functor: TransactionRepository](
      txHash: Signed.TxHash,
  ): EitherT[F, String, Option[TransactionWithResult]] =
    TransactionRepository[F].get(txHash.toResultHashValue).leftMap(_.msg)
