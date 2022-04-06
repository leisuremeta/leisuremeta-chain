package org.leisuremeta.lmchain.core
package node
package service

import java.time.Instant

import scala.concurrent.duration._

import cats.{Monad, Parallel}
import cats.data.EitherT
import cats.effect.Timer
import cats.implicits._

import codec.byte.ByteEncoder
import codec.byte.ByteEncoder.ops._
import crypto.MerkleTrie
import crypto.MerkleTrie.MerkleTrieState
import crypto.Hash.ops._
import datatype.{BigNat, UInt256Refine}
import gossip.GossipClient
import model.Block
import model.Block.ops._
import repository.{BlockRepository, StateRepository, TransactionRepository}
import repository.StateRepository._

object NodeInitializationService {

  def initialize[F[_]
    : Parallel: Monad: LocalGossipService: BlockRepository: TransactionRepository: StateRepository.Name: StateRepository.Token: StateRepository.Balance](
      gossipClients: Map[Int, GossipClient[F]],
      localNodeIndex: Int,
  ): EitherT[F, String, Unit] = for {
    _ <- EitherT.rightT[F, String](scribe.debug(s"Initialize... "))
    _ <- EitherT.rightT[F, String](
      scribe.debug(s"Local node index: $localNodeIndex")
    )
    bestBlockHeaderOption <- BlockRepository[F].bestHeader.leftMap(_.msg)
    _ <- bestBlockHeaderOption match {
      case None =>
        scribe.debug("No best block header found. Initializing genesis block.")
        EitherT.right[String](putGenesisBlockAndTransaction[F])
      case Some(header) =>
        scribe.debug("Best block header found. Skipping genesis block.")
        val blockHash = header.toHash.toBlockHash
        for {
          blockOption <- BlockRepository[F].get(blockHash).leftMap(_.msg)
          block <- EitherT.fromOption[F](
            blockOption,
            s"best block $blockHash not found in the block repository",
          )
          _ <- LocalGossipService[F].setBestConfirmedBlock(blockHash, block)
        } yield ()
    }
    _ <- syncWithPeers[F](gossipClients, localNodeIndex)
  } yield ()

  def putGenesisBlockAndTransaction[F[_]: Monad](implicit
      blockRepo: BlockRepository[F],
      nameStateRepo: StateRepository.Name[F],
      tokenStateRepo: StateRepository.Token[F],
      balanceStateRepo: StateRepository.Balance[F],
  ): F[Unit] = for {
    _ <- blockRepo.put(GenesisBlock).getOrElse {
      throw new Exception(s"Wrong genesis block!!")
    }
    _ <- nameStateRepo.put(MerkleTrieState.empty)
    _ <- tokenStateRepo.put(MerkleTrieState.empty)
    _ <- balanceStateRepo.put(MerkleTrieState.empty)
  } yield ()

  val GenesisBlock: Block = {

    val header = Block.Header(
      number = BigNat.Zero,
      parentHash = UInt256Refine.EmptyBytes.asInstanceOf[Block.BlockHash],
      namesRoot = None,
      tokenRoot = None,
      balanceRoot = None,
      transactionsRoot = None,
      timestamp = Instant.parse("2021-04-23T00:00:00Z"),
    )

    Block(header, Set.empty, Set.empty)
  }

  val GenesisHash: Block.BlockHash = GenesisBlock.toHash

  def syncWithPeers[F[_]
    : Monad: Parallel: LocalGossipService: BlockRepository: TransactionRepository: StateRepository.Name: StateRepository.Token: StateRepository.Balance](
      gossipClients: Map[Int, GossipClient[F]],
      localNodeIndex: Int,
  ): EitherT[F, String, Unit] = for {
    localGossip <- EitherT.right[String](LocalGossipService[F].get)
    localBestConfirmedBlockHeader = localGossip.bestConfirmed._2.header
    bestConfirmedBlockHeaders <- gossipClients.toSeq
      .parTraverse { case (nodeIndex, client) =>
        client.bestConfirmedBlock.map(nodeIndex -> _)
      }
    (bestHeaderNodeIndex, bestHeader) = ((
      localNodeIndex,
      localBestConfirmedBlockHeader,
    ) :: bestConfirmedBlockHeaders.toList)
      .maxBy(_._2.number.value)
    _ <- {
      scribe.debug(s"best header from node $bestHeaderNodeIndex")
      scribe.debug(s"best header number: ${bestHeader.number}")
      scribe.debug(
        s"local best header number: ${localBestConfirmedBlockHeader.number}"
      )
      if (
        bestHeader.number.value === localBestConfirmedBlockHeader.number.value
      ) {
        scribe.debug("Local best comnfirmed block is already global best")
        EitherT.pure[F, String](())
      } else {
        val client    = gossipClients(bestHeaderNodeIndex)
        val blockHash = bestHeader.toHash.toBlockHash

        for {
          block <- pullBlock[F](blockHash, client, bestHeaderNodeIndex)
          _     <- LocalGossipService[F].setBestConfirmedBlock(blockHash, block)
          _     <- pullCurrentState[F](blockHash, bestHeader, client)
        } yield ()
      }
    }
  } yield ()

  def pullBlock[F[_]: Monad: BlockRepository: TransactionRepository](
      blockHash: Block.BlockHash,
      client: GossipClient[F],
      nodeIndex: Int,
  ): EitherT[F, String, Block] =
    for {
      _ <- EitherT.rightT[F, String](scribe.debug(s"Pulling block $blockHash"))
      blockOption <- client.block(blockHash)
      block <- EitherT.fromOption[F](
        blockOption,
        s"Block $blockHash not found on node ${nodeIndex}",
      )
      txs <- client.txs(block.transactionHashes)
      txMap = txs.map(tx => tx.toHash -> tx).toMap
      _ <- EitherT.cond[F](
        block.transactionHashes.forall(txMap.contains),
        (),
        s"Missed txs: ${block.transactionHashes -- txMap.keySet}",
      )
      _ <- BlockService.saveBlock[F](block, txMap)
    } yield block

  def pullCurrentState[F[_]
    : Monad: StateRepository.Name: StateRepository.Token: StateRepository.Balance](
      blockHash: Block.BlockHash,
      header: Block.Header,
      client: GossipClient[F],
  ): EitherT[F, String, Unit] = {
    val state = StateService.MerkleState.from(header)
    val name = pullState(client.nameState(blockHash, None, None))(
      state.namesState
    )
    val token = pullState(client.tokenState(blockHash, None, None))(
      state.tokenState
    )
    val balance = pullState(
      client
        .balanceState(blockHash, None, None)
        .map(_.map { case (key, value) =>
          ((key, value), ())
        })
    )(state.balanceState)

    for {
      name    <- name
      token   <- token
      balance <- balance
    } yield {
      scribe.debug(s"Pulled state: $name, $token, $balance")
      ()
    }
  }

  def pullState[F[_]: Monad, K: ByteEncoder, V: ByteEncoder](
      getItems: EitherT[F, String, List[(K, V)]]
  )(
      stateRoot: MerkleTrieState[K, V]
  )(implicit
      stateRepo: StateRepository[F, K, V]
  ): EitherT[F, String, Unit] = for {
    items <- getItems
    _     <- EitherT.pure[F, String](scribe.debug(s"state item: $items"))
    finalState <- items.foldLeftM(MerkleTrieState.empty[K, V]) {
      case (state, (k, v)) =>
        MerkleTrie.put[F, K, V](k.toBytes.bits, v).runS(state)
    }
    _ <- EitherT.cond[F](
      finalState == stateRoot,
      (),
      s"Wrong state root! expected $stateRoot but get $finalState",
    )
    _ <- EitherT.right[String](stateRepo.put(finalState))
  } yield ()

  def pullAncestorBlocksByLocalGossip[F[_]
    : Monad: Timer: LocalGossipService: BlockRepository: TransactionRepository: StateRepository.Name: StateRepository.Token: StateRepository.Balance](
      pullingPeriodMillis: Long,
      gossipClients: Map[Int, GossipClient[F]],
  ): F[Unit] = for {
    localGossip <- LocalGossipService[F].get
    (blockHash, block) = localGossip.bestConfirmed
    _ <- Monad[F].pure(
      scribe.debug(s"Start pulling ancestor blocks from $blockHash: $block")
    )
    _ <- pullAncestorBlocks[F](
      number = block.header.number,
      blockHash = blockHash,
      pullingPeriodMillis = pullingPeriodMillis,
      gossipClients = gossipClients,
    )
  } yield ()

  def pullAncestorBlocks[F[_]
    : Monad: Timer: BlockRepository: TransactionRepository: StateRepository.Name: StateRepository.Token: StateRepository.Balance](
      number: BigNat,
      blockHash: Block.BlockHash,
      pullingPeriodMillis: Long,
      gossipClients: Map[Int, GossipClient[F]],
  ): F[Unit] = if (number.value <= 0) Monad[F].pure(())
  else {
    scribe.debug(s"Pulling block #$number: $blockHash")
    BlockRepository[F]
      .get(blockHash)
      .value
      .flatMap {
        case Right(Some(block)) =>
          val number1 = BigNat.unsafeFromBigInt(number.value - 1)
          Monad[F].pure((number1, block.header.parentHash))
        case other =>
          scribe.debug(s"fail to get local block $blockHash: $other")
          val randomIndex = scala.util.Random.between(0, gossipClients.size)
          val (clientIndex, client) = gossipClients.toIndexedSeq(randomIndex)
          scribe.debug(
            s"Pulling ancestor blocks for $blockHash from node $clientIndex"
          )
          pullBlock[F](blockHash, client, clientIndex).value.map {
            case Right(block1) =>
              val number1 = BigNat.unsafeFromBigInt(number.value - 1)
              (number1, block1.header.parentHash)
            case Left(msg) =>
              scribe.debug(
                s"fail to pull ancestor block $blockHash from node $clientIndex: $msg"
              )
              (number, blockHash)
          }
      }
      .flatMap { case (number1, blockHash1) =>
        scribe.debug(s"Pulled block #$number1: $blockHash1")
        Timer[F].sleep(pullingPeriodMillis.millis) >>
          pullAncestorBlocks[F](
            number1,
            blockHash1,
            pullingPeriodMillis,
            gossipClients,
          )
      }
  }
}
