package io.leisuremeta.chain
package node

import java.time.Instant

import cats.Monad
import cats.data.{EitherT, Kleisli}
import cats.implicits.*

import api.model.{
  Account,
  PublicKeySummary,
  Block,
  Signed,
  StateRoot,
  TransactionWithResult,
}
import api.model.Block.ops.*
import lib.crypto.{KeyPair, Recover, Sign, Signature}
import lib.crypto.Hash.ops.*
import lib.crypto.Recover.ops.*
import lib.crypto.Sign.ops.*
import lib.datatype.BigNat
import lib.merkle.{MerkleTrie, MerkleTrieNode, MerkleTrieState}
import lib.merkle.MerkleTrie.NodeStore
import lib.merkle.MerkleTrieNode.MerkleRoot

object GossipDomain:

  case class MerkleState(
      namesState: MerkleTrieState[Account, Option[Account]],
      keyState: MerkleTrieState[
        (Account, PublicKeySummary),
        PublicKeySummary.Info,
      ],
  ):
    def toStateRoot: StateRoot = StateRoot(
      account = StateRoot.AccountStateRoot(
        namesRoot = namesState.root,
        keyRoot = keyState.root,
      ),
    )

  object MerkleState:
    def from(header: Block.Header): MerkleState = MerkleState(
      namesState = buildMerkleTrieState(header.stateRoot.account.namesRoot),
      keyState = buildMerkleTrieState(header.stateRoot.account.keyRoot),
    )

  def buildMerkleTrieState[K, V](
      root: Option[MerkleRoot[K, V]],
  ): MerkleTrieState[K, V] =
    root.fold(MerkleTrieState.empty[K, V])(MerkleTrieState.fromRoot)

  case class LocalGossip(
      newTxs: Map[Signed.TxHash, Signed.Tx],
      newBlockSuggestions: Map[Block.BlockHash, (Block, MerkleState)],
      newBlockVotes: Map[(Block.BlockHash, Int), Signature],
      idxTxBlockContains: Map[Signed.TxHash, Set[Block.BlockHash]],
      idxTxBlockSupporting: Map[Signed.TxHash, Set[Block.BlockHash]],
      idxBlockSupportingTx: Map[Block.BlockHash, Set[Signed.TxHash]],
      idxBlockNumberBlock: Map[BigNat, Set[Block.BlockHash]],
      idxBlockNumberVote: Map[(BigNat, Int), Set[Block.BlockHash]],
      bestBlock: Option[(Block.BlockHash, (Block, MerkleState))],
      bestConfirmed: (Block.BlockHash, Block),
      lastConfirmed: List[(Block.BlockHash, Block)],
  )
  object LocalGossip:
    def empty(bestConfirmedBlock: Block): LocalGossip =
      LocalGossip(
        newTxs = Map.empty,
        newBlockSuggestions = Map.empty,
        newBlockVotes = Map.empty,
        idxTxBlockContains = Map.empty,
        idxTxBlockSupporting = Map.empty,
        idxBlockSupportingTx = Map.empty,
        idxBlockNumberBlock = Map.empty,
        idxBlockNumberVote = Map.empty,
        bestBlock = None,
        bestConfirmed = (bestConfirmedBlock.toHash, bestConfirmedBlock),
        lastConfirmed = List.empty,
      )

  case class GossipParams(
      nodeAddresses: Map[Int, PublicKeySummary],
      timeWindowMillis: Long,
      localKeyPair: KeyPair,
  ):
    val localAddress: PublicKeySummary =
      PublicKeySummary.fromPublicKeyHash(localKeyPair.publicKey.toHash)
    val localNodeIndex = nodeAddresses
      .find(_._2 === localAddress)
      .getOrElse(
        throw new IllegalArgumentException(
          s"Local node address $localAddress not found in node addresses $nodeAddresses",
        ),
      )
      ._1

  @FunctionalInterface
  trait UpdateState[F[_]]:
    def apply(
        state: MerkleState,
        tx: Signed.Tx,
    ): EitherT[F, String, (MerkleState, TransactionWithResult)]
  object UpdateState:
    def apply[F[_]](using us: UpdateState[F]): UpdateState[F] = us

  def onNewTx[F[_]: Monad](gossip: LocalGossip, tx: Signed.Tx)(using
      updateState: UpdateState[F],
  ): EitherT[F, String, LocalGossip] =

    val (bestConfirmedHash, bestConfirmedBlock) = gossip.bestConfirmed
    val bestConfirmedBlockState: MerkleState =
      MerkleState.from(bestConfirmedBlock.header)
    val headers: List[(Block.BlockHash, (Block, MerkleState))] =
      (bestConfirmedHash, (bestConfirmedBlock, bestConfirmedBlockState)) ::
        gossip.newBlockSuggestions.toList

    for
      isSupportings <- EitherT.right[String](
        headers.traverse { case (blockHash, (block @ _, state)) =>
          updateState(state, tx).value.map {
            case Right(_) => true
            case Left(msg) =>
              scribe.info(
                s"block $blockHash does not support tx ${tx.toHash}: $msg",
              )
              false
          }
        },
      )
      headerWithSupportingFlags = headers.zip(isSupportings)
      ans = headerWithSupportingFlags.filter(_._2).nonEmpty
      _ <- EitherT.cond[F](ans, (), s"No supporting block header found")
    yield
      val txHash: Signed.TxHash = tx.toHash
      val newTxs1: Map[Signed.TxHash, Signed.Tx] =
        gossip.newTxs + (txHash -> tx)
      val idxTxBlockSupporting1: Map[Signed.TxHash, Set[Block.BlockHash]] =
        gossip.idxTxBlockSupporting +
          (txHash -> headerWithSupportingFlags.filter(_._2).map(_._1._1).toSet)
      val idxBlockSupportingTx1: Map[Block.BlockHash, Set[Signed.TxHash]] =
        headerWithSupportingFlags
          .filter(_._2)
          .map(_._1._1)
          .toSet
          .foldLeft(gossip.idxBlockSupportingTx) { case (idx, blockHash) =>
            idx + (blockHash -> (idx.getOrElse(blockHash, Set.empty) + txHash))
          }
      gossip.copy(
        newTxs = newTxs1,
        idxTxBlockSupporting = idxTxBlockSupporting1,
        idxBlockSupportingTx = idxBlockSupportingTx1,
      )

  def generateNewBlockSuggestion[F[_]: Monad](
      gossip: LocalGossip,
      currentTime: Instant,
      params: GossipParams,
  )(using
      updateState: UpdateState[F],
  ): EitherT[F, String, (Block.BlockHash, Block)] =
    val (bestBlockHash, (bestBlock, bestState)) = gossip.bestBlock.getOrElse {
      val (bestConfirmedHash, bestConfirmedBlock) = gossip.bestConfirmed
      (
        bestConfirmedHash,
        (bestConfirmedBlock, MerkleState.from(bestConfirmedBlock.header)),
      )
    }

    val candidateTxs =
      gossip.idxBlockSupportingTx.getOrElse(bestBlockHash, Set.empty)

    if candidateTxs.isEmpty && bestBlock.transactionHashes.isEmpty then
      EitherT.leftT[F, (Block.BlockHash, Block)](
        "No transactions to include in new block",
      )
    else
      given idNodeStore: NodeStore[cats.Id, Signed.TxHash, Unit] =
        Kleisli.pure(None)
      for
        stateAndTxSet <- candidateTxs.toList.foldM(
          (bestState, Set.empty[Signed.TxHash]),
        ) { case ((state, txSet), txHash) =>
          (for
            txOption <- EitherT.rightT[F, String](gossip.newTxs.get(txHash))
            tx <- EitherT.fromOption[F](txOption, s"tx not found: $txHash")
            stateAndTxResult <- updateState(state, tx)
            state1 = stateAndTxResult._1
          yield (state1, txSet + txHash)).recover { _ => (state, txSet) }
        }
        (state, txSet) = stateAndTxSet
        txState = txSet.toList.foldLeft(
          MerkleTrieState.empty[Signed.TxHash, Unit],
        ) { (state, txHash) =>
          MerkleTrie
            .put[cats.Id, Signed.TxHash, Unit](
              txHash.toUInt256Bytes.toBytes.bits,
              (),
            )
            .runS(state)
            .value
            .getOrElse(state)
        }
        header = Block.Header(
          number = BigNat.add(bestBlock.header.number, BigNat.One),
          parentHash = bestBlockHash,
          stateRoot = state.toStateRoot,
          transactionsRoot = txState.root,
          timestamp = currentTime,
        )
        headerHash = header.toHash
        blockHash  = headerHash.toBlockHash
        sig <- EitherT.fromEither[F](
          Sign[Block.Header].byHash(headerHash, params.localKeyPair),
        )
      yield (
        blockHash,
        Block(
          header = header,
          transactionHashes = txSet,
          votes = Set(sig),
        ),
      )

  def onNewBlockSuggestion[F[_]: Monad](
      gossip: LocalGossip,
      block: Block,
      params: GossipParams,
  )(using
      updateState: UpdateState[F],
  ): EitherT[F, String, (LocalGossip, Option[Signature])] =

    val blockHash: Block.BlockHash = block.toHash

    val parentStateEither: Either[String, MerkleState] =
      if block.header.parentHash === gossip.bestConfirmed._1 then
        Right(MerkleState.from(gossip.bestConfirmed._2.header))
      else
        gossip.newBlockSuggestions
          .get(block.header.parentHash)
          .fold[Either[String, MerkleState]](
            Left(s"Parent block ${block.header.parentHash} not found in gossip"),
          ) { case (_, state) => Right(state) }

    val txsEither: Either[String, List[Signed.Tx]] =
      block.transactionHashes.toList.traverse { (txHash) =>
        gossip.newTxs
          .get(txHash)
          .toRight(s"Transaction $txHash not found in gossip")
      }

    for
      parentState <- EitherT.fromEither[F](parentStateEither)
      txs         <- EitherT.fromEither[F](txsEither)
      newState    <- txs.foldLeftM(parentState)(updateState(_, _).map(_._1))
      providerIndex <- EitherT.fromEither[F](
        checkBlockValidityAndGetProviderIndex(gossip, blockHash, block, params),
      )
      gossip1 <- EitherT.right[String](
        saveBlockSuggestion[F](gossip, blockHash, block, newState),
      )
      isLocal: Boolean   = providerIndex == params.localNodeIndex
      isVotable: Boolean = !isLocal && isVotableBlockSuggestion(gossip1, block)
      sigOption <-
        if !isVotable then EitherT.pure[F, String](None)
        else
          EitherT
            .fromEither[F](blockHash.toHeaderHash.signBy(params.localKeyPair))
            .map(Some(_))
    yield
      val bestBlock1 =
        if isLocal then Some((blockHash, (block, newState)))
        else gossip1.bestBlock
      (gossip1.copy(bestBlock = bestBlock1), sigOption)

  private def checkBlockValidityAndGetProviderIndex(
      gossip: LocalGossip,
      blockHash: Block.BlockHash,
      block: Block,
      params: GossipParams,
  ): Either[String, Int] = for
    sig <- Either.cond(
      block.votes.size === 1,
      block.votes.head,
      s"block suggestion ${blockHash} expect to have only one votes, but: ${block.votes.size}",
    )
    publicKey <- Either.fromOption(
      Recover[Block.Header].fromHash(blockHash.toHeaderHash, sig),
      s"block suggestion ${block.toHash} has invalid signature",
    )
    providerPubkey = PublicKeySummary.fromPublicKeyHash(publicKey.toHash)
    providerIndex <- params.nodeAddresses
      .find(_._2 === providerPubkey)
      .map(_._1)
      .toRight(
        s"provider address is not found: $providerPubkey",
      )
    timeWindowNumber: Long =
      block.header.timestamp.toEpochMilli() / params.timeWindowMillis
    hasAuthority =
      (timeWindowNumber % params.nodeAddresses.size).toInt === providerIndex
    _ <- Either.cond(
      hasAuthority,
      (),
      s"block ${blockHash} has invalid authority:\n"
        + s"provider public key summary: $providerPubkey\n"
        + s"provider index: $providerIndex\n"
        + s"time window number $timeWindowNumber",
    )
    _ <- Either.cond(
      isDescendant(gossip, gossip.bestConfirmed._1, block.header),
      (),
      s"block ${blockHash} is not descendant of best confirmed block",
    )
  yield providerIndex

  @annotation.tailrec
  private def isDescendant(
      gossip: LocalGossip,
      ancestorHash: Block.BlockHash,
      blockHeader: Block.Header,
  ): Boolean =
    val parentHash: Block.BlockHash = blockHeader.parentHash
    if parentHash === ancestorHash then true
    else
      gossip.newBlockSuggestions.get(parentHash) match
        case None => false
        case Some((parentBlock, _)) =>
          isDescendant(gossip, ancestorHash, parentBlock.header)

  private def isVotableBlockSuggestion(
      gossip: LocalGossip,
      blockSuggestion: Block,
  ): Boolean = gossip.bestBlock.fold(true) { case (_, (bestBlock, _)) =>
    blockSuggestion.header.number.toBigInt > bestBlock.header.number.toBigInt
  }

  private def saveBlockSuggestion[F[_]: Monad: UpdateState](
      gossip: LocalGossip,
      blockHash: Block.BlockHash,
      blockSuggestion: Block,
      state: MerkleState,
  ): F[LocalGossip] =
    val newBlockSuggestions1 = gossip.newBlockSuggestions.updated(
      key = blockHash,
      value = (blockSuggestion, state),
    )
    val idxTxBlockContains1 =
      blockSuggestion.transactionHashes.foldLeft(gossip.idxTxBlockContains) {
        case (idx, txHash) =>
          idx.updated(txHash, idx.getOrElse(txHash, Set.empty) + blockHash)
      }

    updateIdxTxBlockSupporting[F](gossip, blockHash, blockSuggestion, state)
      .map { idxTxBlockSupporting =>
        gossip.copy(
          newBlockSuggestions = newBlockSuggestions1,
          idxTxBlockContains = idxTxBlockContains1,
          idxTxBlockSupporting = idxTxBlockSupporting,
        )
      }

  private def updateIdxTxBlockSupporting[F[_]: Monad](
      gossip: LocalGossip,
      blockSuggestionHash: Block.BlockHash,
      blockSuggestion: Block,
      blockSuggestionState: MerkleState,
  )(using
      updateState: UpdateState[F],
  ): F[Map[Signed.TxHash, Set[Block.BlockHash]]] =
    (gossip.newTxs -- blockSuggestion.transactionHashes).toList
      .traverseFilter { case (txHash, tx) =>
        updateState(blockSuggestionState, tx).value.map(
          _.toOption.map(_ => txHash),
        )
      }
      .map { (txHashes: List[Signed.TxHash]) =>
        txHashes.foldLeft(gossip.idxTxBlockSupporting) { case (idx, txHash) =>
          idx.updated(
            txHash,
            idx.getOrElse(txHash, Set.empty) + blockSuggestionHash,
          )
        }
      }

  def onNewBlockVote(
      gossip: LocalGossip,
      blockHash: Block.BlockHash,
      nodeNo: Int,
      sig: Signature,
      params: GossipParams,
  ): Either[String, LocalGossip] = for
    blockAndState <- getBlock(gossip, blockHash)
    (block, state) = blockAndState
    _ <- Either.cond(
      block.votes =!= Set(sig),
      (),
      s"suggestor's vote: will be ignored",
    )
    _ <- checkBlockVoteSignature(blockHash, nodeNo, sig, params)
  yield
    val newBlockVotes1 = gossip.newBlockVotes + ((blockHash, nodeNo) -> sig)
    val idxBlockNumberVote1 = gossip.idxBlockNumberVote.updated(
      key = (block.header.number, nodeNo),
      value = gossip.idxBlockNumberVote.getOrElse(
        (block.header.number, nodeNo),
        Set.empty,
      ) + blockHash,
    )
    val bestBlock1 =
      if nodeNo === params.localNodeIndex then Some((blockHash, (block, state)))
      else gossip.bestBlock
    gossip.copy(
      newBlockVotes = newBlockVotes1,
      idxBlockNumberVote = idxBlockNumberVote1,
      bestBlock = bestBlock1,
    )

  private def getBlock(
      gossip: LocalGossip,
      blockHash: Block.BlockHash,
  ): Either[String, (Block, MerkleState)] = Either
    .fromOption(
      gossip.newBlockSuggestions.get(blockHash),
      s"block suggestion $blockHash not found",
    )

  private def checkBlockVoteSignature(
      blockHash: Block.BlockHash,
      nodeNo: Int,
      sig: Signature,
      params: GossipParams,
  ): Either[String, Unit] = for
    publicKey <- Either.fromOption(
      blockHash.toHeaderHash.recover(sig),
      s"invalid signature: $sig",
    )
    address     = PublicKeySummary.fromPublicKeyHash(publicKey.toHash)
    nodeAddress = params.nodeAddresses(nodeNo)
    _ <- Either.cond(
      address === nodeAddress,
      (),
      s"invalid address: $address, expected: $nodeAddress",
    )
  yield ()

  final case class BlockFinalizationResult(
      finalizedBlocks: List[(Block.BlockHash, Block)],
      removedTxHashes: Set[Signed.TxHash],
  )

  def tryFinalizeBlockWithBlockHash(
      gossip: LocalGossip,
      blockHash: Block.BlockHash,
      params: GossipParams,
  ): Either[String, (LocalGossip, BlockFinalizationResult)] =
    getBlock(gossip, blockHash).flatMap { case (block, _) =>
      tryFinalizeBlockWithBlock(gossip, blockHash, block, params)
    }

  def tryFinalizeBlockWithBlock(
      gossip: LocalGossip,
      blockHash: Block.BlockHash,
      block: Block,
      params: GossipParams,
  ): Either[String, (LocalGossip, BlockFinalizationResult)] =
    def validVote(index: Int): Boolean =
      gossip.idxBlockNumberVote.get((block.header.number, index)) match
        case Some(set) if set.size === 1 => true
        case _                           => false

    val validVoterIndexes = params.nodeAddresses.keySet
      .filter(i => gossip.newBlockVotes.contains((blockHash, i)))
      .filter(validVote(_))

    val numberOfValidVote = validVoterIndexes.size

    val isConfirmed: Boolean =
      (numberOfValidVote + 1) * 3 > params.nodeAddresses.keySet.size * 2

    scribe.debug(s"block $blockHash is confirmed: $isConfirmed")

    if isConfirmed then
      finalizeBlockAndItsAncestors(gossip, blockHash, block, params, Nil)
    else
      Left(
        s"not enough number of vote to finalize: currently $numberOfValidVote",
      )

  private def finalizeBlockAndItsAncestors(
      gossip: LocalGossip,
      blockHash: Block.BlockHash,
      block: Block,
      params: GossipParams,
      acc: List[(Block.BlockHash, Block)],
  ): Either[String, (LocalGossip, BlockFinalizationResult)] =

    val parentHash = block.header.parentHash

    if gossip.bestConfirmed._1 === parentHash then

      val blocksToFinalize = ((blockHash, block) :: acc).map {
        case (blockHash, block) =>
          val sigsToAdd =
            gossip.newBlockVotes.filter(_._1._1 === blockHash).map(_._2)
          (blockHash, block.copy(votes = block.votes ++ sigsToAdd))
      }
      val txHashSetToFinalize =
        blocksToFinalize.foldLeft(Set.empty[Signed.TxHash]) {
          _ ++ _._2.transactionHashes
        }
      val bestConfirmed1         = blocksToFinalize.last
      val blockHashSetToFinalize = blocksToFinalize.map(_._1).toSet
      val blockNumberSetToRemove =
        blocksToFinalize.map(_._2.header.number).toSet
      val blockSetToRemove =
        (for
          (blockHash, (block, _)) <- gossip.newBlockSuggestions
          if !blockHashSetToFinalize.contains(blockHash) &&
            blockNumberSetToRemove.contains(block.header.number)
        yield (blockHash, block)).toSet ++ gossip.lastConfirmed.toSet
      val blockHashSetToRemove = blockSetToRemove.map(_._1)
      val blockHashSetToRemoveInSupporting =
        (blockSetToRemove - bestConfirmed1).map(_._1)
      val txHashSetToRemove: Set[Signed.TxHash] = blockSetToRemove.flatMap {
        case (_, block) => block.transactionHashes
      } ++ txHashSetToFinalize

      val newTxs1 = gossip.newTxs -- txHashSetToRemove
      val newBlockSuggestions1 =
        gossip.newBlockSuggestions -- blockHashSetToRemove
      val newBlockVotes1 = gossip.newBlockVotes.filterNot {
        case ((blockHash, _), _) => blockHashSetToRemove.contains(blockHash)
      }
      val idxTxBlockContains1 = gossip.idxTxBlockContains -- txHashSetToRemove
      val idxTxBlockSupporting1 =
        (gossip.idxTxBlockSupporting -- txHashSetToRemove).view
          .mapValues(_ -- blockHashSetToRemoveInSupporting)
          .toMap
      val idxBlockSupportingTx1 =
        (gossip.idxBlockSupportingTx -- blockHashSetToRemoveInSupporting).view
          .mapValues(_ -- txHashSetToRemove)
          .filterNot(_._2.isEmpty)
          .toMap
      val idxBlockNumberBlock1 =
        gossip.idxBlockNumberBlock -- blockNumberSetToRemove
      val idxBlockNumberVote1 = gossip.idxBlockNumberVote.filterNot {
        case ((blockNumber, _), _) =>
          blockNumberSetToRemove.contains(blockNumber)
      }

      for
        bestConfirmedState <- Either.fromOption(
          gossip.newBlockSuggestions.get(bestConfirmed1._1).map(_._2),
          s"best confirmed block $bestConfirmed1 not found",
        )
        newBlockSuggestionsList <- newBlockSuggestions1.toList.traverse {
          case (blockHash, (block, state)) =>
            rebaseState(state, bestConfirmedState).map { state =>
              (blockHash, (block, state))
            }
        }
      yield

        val newBlockSuggestions2 = newBlockSuggestionsList.toMap

        val bestBlock1 = gossip.bestBlock match
          case None => None
          case Some((blockHash, (bestBlock, _))) =>
            if isDescendant(gossip, bestConfirmed1._1, bestBlock.header) then
              val (block, state) = newBlockSuggestions2(blockHash)
              Some((blockHash, (block, state)))
            else None
        val gossip1 = gossip.copy(
          newTxs = newTxs1,
          newBlockSuggestions = newBlockSuggestions2,
          newBlockVotes = newBlockVotes1,
          idxTxBlockContains = idxTxBlockContains1,
          idxTxBlockSupporting = idxTxBlockSupporting1,
          idxBlockSupportingTx = idxBlockSupportingTx1,
          idxBlockNumberBlock = idxBlockNumberBlock1,
          idxBlockNumberVote = idxBlockNumberVote1,
          bestBlock = bestBlock1,
          bestConfirmed = bestConfirmed1,
          lastConfirmed = blocksToFinalize,
        )
        (
          gossip1,
          BlockFinalizationResult(
            blocksToFinalize,
            txHashSetToRemove -- txHashSetToFinalize,
          ),
        )
    else
      getBlock(gossip, parentHash).flatMap { case (parent, _) =>
        finalizeBlockAndItsAncestors(
          gossip,
          parentHash,
          parent,
          params,
          (blockHash, block) :: acc,
        )
      }

  private def rebaseState(
      state: MerkleState,
      newBase: MerkleState,
  ): Either[String, MerkleState] =
    for
      namesState <- state.namesState.rebase(newBase.namesState)
      keyState   <- state.keyState.rebase(newBase.keyState)
    yield MerkleState(namesState, keyState)
