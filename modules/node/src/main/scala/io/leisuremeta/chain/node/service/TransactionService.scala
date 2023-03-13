package io.leisuremeta.chain
package node
package service

import cats.{Functor, Monad}
import cats.data.{EitherT, Kleisli, StateT}
import cats.effect.{Clock, Concurrent}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*

import api.model.{Block, Signed, Transaction, TransactionWithResult}
import api.model.Block.ops.*
import api.model.TransactionWithResult.ops.*
import api.model.api_model.TxInfo
import dapp.PlayNommState
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
import repository.{BlockRepository, GenericStateRepository, StateRepository, TransactionRepository}
import state.UpdateState
import GossipDomain.MerkleState

object TransactionService:
  def submit[F[_]: Concurrent: Clock: BlockRepository: TransactionRepository: PlayNommState: GenericStateRepository.AccountState: GenericStateRepository.GroupState: GenericStateRepository.TokenState: GenericStateRepository.RewardState](
      txs: Seq[Signed.Tx],
      localKeyPair: KeyPair,
  ): EitherT[F, String, Seq[Hash.Value[TransactionWithResult]]] = for
    bestBlockHeaderOption <- BlockRepository[F].bestHeader.leftMap { e =>
      scribe.error(s"Best Header Error: $e")
      e.msg
    }
    bestBlockHeader <- EitherT.fromOption[F](
      bestBlockHeaderOption,
      "No best Header Available",
    )
    baseState = MerkleState.from(bestBlockHeader)
    result <- txs
      .traverse { tx =>
        StateT[EitherT[F, String, *], MerkleState, TransactionWithResult] { (ms: MerkleState) =>
          tx.value match
            case txv: Transaction.AccountTx =>
              UpdateState[F, Transaction.AccountTx](ms, tx.sig, txv)
            case txv: Transaction.GroupTx =>
              UpdateState[F, Transaction.GroupTx](ms, tx.sig, txv)
            case txv: Transaction.TokenTx =>
              UpdateState[F, Transaction.TokenTx](ms, tx.sig, txv)
            case txv: Transaction.RewardTx =>
              UpdateState[F, Transaction.RewardTx](ms, tx.sig, txv)
        }
      }
      .run(baseState)
    (finalState, txWithResults) = result
    txHashes                    = txWithResults.map(_.toHash)
    txState = txs
      .map(_.toHash)
      .foldLeft(
        GenericMerkleTrieState.empty[Signed.TxHash, Unit],
      ) { (state, txHash) =>
        given idNodeStore: GenericNodeStore[cats.Id, Signed.TxHash, Unit] =
          Kleisli.pure(None)
        GenericMerkleTrie
          .put[cats.Id, Signed.TxHash, Unit](
            txHash.toUInt256Bytes.toBytes.bits,
            (),
          )
          .runS(state)
          .value
          .getOrElse(state)
      }
    stateRoot = finalState.toStateRoot
    now <- EitherT.right(Clock[F].realTimeInstant)
    header = Block.Header(
      number = BigNat.add(bestBlockHeader.number, BigNat.One),
      parentHash = bestBlockHeader.toHash.toBlockHash,
      stateRoot = stateRoot,
      transactionsRoot = txState.root,
      timestamp = now,
    )
    sig <- EitherT.fromEither(header.toHash.signBy(localKeyPair))
    block = Block(
      header = header,
      transactionHashes = txHashes.toSet.map(_.toSignedTxHash),
      votes = Set(sig),
    )
    _ <- BlockRepository[F].put(block).leftMap(_.msg)

    _ <- EitherT.right[String](
      List(
        GenericStateRepository.AccountState[F].key.put(finalState.account.keyState),
        GenericStateRepository.AccountState[F].eth.put(finalState.account.ethState),
        GenericStateRepository.GroupState[F].group.put(finalState.group.groupState),
        GenericStateRepository.GroupState[F].groupAccount.put(finalState.group.groupAccountState),
        GenericStateRepository.TokenState[F].definition.put(finalState.token.tokenDefinitionState),
        GenericStateRepository.TokenState[F].fungibleBalance.put(finalState.token.fungibleBalanceState),
        GenericStateRepository.TokenState[F].nftBalance.put(finalState.token.nftBalanceState),
        GenericStateRepository.TokenState[F].nft.put(finalState.token.nftState),
        GenericStateRepository.TokenState[F].rarity.put(finalState.token.rarityState),
        GenericStateRepository.TokenState[F].entrustFungibleBalance.put(finalState.token.entrustFungibleBalanceState),
        GenericStateRepository.TokenState[F].entrustNftBalance.put(finalState.token.entrustNftBalanceState),
        GenericStateRepository.RewardState[F].daoState.put(finalState.reward.daoState),
        GenericStateRepository.RewardState[F].userActivityState.put(finalState.reward.userActivityState),
        GenericStateRepository.RewardState[F].tokenReceivedState.put(finalState.reward.tokenReceivedState),
      ).sequence
    )
    _ <- txWithResults.traverse { txWithResult =>
      EitherT.right(TransactionRepository[F].put(txWithResult))
    }
  yield txHashes

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
