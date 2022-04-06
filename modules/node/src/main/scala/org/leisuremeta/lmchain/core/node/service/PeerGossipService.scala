package org.leisuremeta.lmchain.core
package node
package service

import cats.data.{EitherT, OptionT}
import cats.effect.Effect
import cats.implicits._

import monix.tail.Iterant
import scodec.bits.{BitVector, ByteVector}

import codec.byte.{ByteDecoder, ByteEncoder, DecodeResult}
import crypto.{MerkleTrie, Signature}
import crypto.MerkleTrie.NodeStore
import crypto.MerkleTrieNode.MerkleRoot
import gossip.{BloomFilter, GossipApi}
import model.{Account, Block, NameState, Signed, TokenState, Transaction}
import repository.{BlockRepository, StateRepository, TransactionRepository}
import repository.StateRepository._

object PeerGossipService {
  def gossipApi[F[_]
    : Effect: LocalGossipService: BlockRepository: StateRepository.Name: StateRepository.Token: StateRepository.Balance: TransactionRepository]
      : GossipApi[F] =
    new GossipApi[F] {

      def bestConfirmedBlock: F[Block.Header] =
        LocalGossipService[F].get.map(_.bestConfirmed._2.header)
      def block(blockHash: Block.BlockHash): F[Option[Block]] = {
        OptionT(
          LocalGossipService[F].get.map(
            _.newBlockSuggestions.get(blockHash).map(_._1)
          )
        ).orElseF {
          BlockRepository[F].get(blockHash).getOrElseF {
            Effect[F].raiseError(
              new RuntimeException(s"Block $blockHash is failed to decode.")
            )
          }
        }.value
      }

      private def getStateItems[K: ByteDecoder: ByteEncoder, V: ByteDecoder](
          blockHash: Block.BlockHash,
          from: Option[K],
          limit: Option[Int],
      )(getRoot: Block.Header => Option[MerkleRoot[K, V]])(implicit
          ns: NodeStore[F, K, V]
      ): F[List[(K, V)]] = {

        def raiseError[A](msg: String): F[A] = {
          scribe.debug(msg)
          Effect[F].raiseError(new RuntimeException(msg))
        }

        for {
          blockOption <- block(blockHash)
          block <- blockOption.fold(
            raiseError[Block](s"No block found for $blockHash")
          )(Effect[F].pure)
          state = StateService.buildMerkleTrieState(getRoot(block.header))
          states <- MerkleTrie
            .from[F, K, V] {
              from
                .fold(ByteVector.empty)(ByteEncoder[K].encode)
                .bits
            }
            .runA(state)
            .value
          _ <- Effect[F].delay(
            scribe.debug(
              s"Get states: ${states}"
            )
          )
          items <- (states match {
            case Right(iter) =>
              iterToList(iter, limit){msg =>
                raiseError[List[(BitVector, V)]](msg)
              }
            case Left(msg) =>
              Effect[F].raiseError[List[(BitVector, V)]](
                new Exception(s"$msg")
              )
          })
          _ <- Effect[F].delay(
            scribe.debug(
              s"Get state items: ${items}"
            )
          )
          itemsDecoded <- items.traverse { case (bits, state) =>
            ByteDecoder[K].decode(bits.bytes) match {
              case Left(failure) =>
                raiseError[(K, V)](s"State key decode failure: ${failure.msg}")
              case Right(DecodeResult(name, _)) =>
                Effect[F].pure((name, state))
            }
          }
        } yield {
          scribe.debug(s"Get state items result: $itemsDecoded")
          itemsDecoded
        }
      }

      def iterToList[A](
          iter: Iterant[EitherT[F, String, *], A],
          limit: Option[Int],
      )(raiseError: String => F[List[A]]): F[List[A]] = {
        scribe.debug(s"iterToList: $iter, $limit")
        limit.fold(iter)(iter.take)
          .toListL
          .map{ list =>
            scribe.debug(s"iterToList toListL result: $list")
            list
          }
          .leftSemiflatMap[List[A]](raiseError)
          .merge
          .map{ list =>
            scribe.debug(s"iterToList result: $list")
            list
          }
      }

      def nameState(
          blockHash: Block.BlockHash,
          from: Option[Account.Name],
          limit: Option[Int],
      ): F[List[(Account.Name, NameState)]] = {
        scribe.debug(s"getting name state items of block $blockHash")
        getStateItems[Account.Name, NameState](blockHash, from, limit)(
          _.namesRoot
        )
      }

      def tokenState(
          blockHash: Block.BlockHash,
          from: Option[Transaction.Token.DefinitionId],
          limit: Option[Int],
      ): F[List[(Transaction.Token.DefinitionId, TokenState)]] = {
        scribe.debug(s"getting token state items of block $blockHash")
        getStateItems[Transaction.Token.DefinitionId, TokenState](
          blockHash,
          from,
          limit,
        )(_.tokenRoot)
      }

      def balanceState(
          blockHash: Block.BlockHash,
          from: Option[(Account, Transaction.Input.Tx)],
          limit: Option[Int],
      ): F[List[(Account, Transaction.Input.Tx)]] = {
        scribe.debug(s"getting balance state items of block $blockHash")
        getStateItems[(Account, Transaction.Input.Tx), Unit](
          blockHash,
          from,
          limit,
        )(_.balanceRoot).map { list =>
          list.map { case ((account, tx), _) => (account, tx) }
        }
      }

      def newTxAndBlockSuggestions(
          bloomFilter: BloomFilter
      ): F[(Set[Signed.Tx], Set[Block])] = LocalGossipService[F].get.map {
        localGossip =>
          val newTxsSet =
            localGossip.newTxs.filterNot(bloomFilter `check` _._1).values.toSet
          val newBlockSuggestionsSet =
            localGossip.newBlockSuggestions.view
              .mapValues(_._1)
              .filterNot(bloomFilter `check` _._1)
              .values
              .toSet
          (newTxsSet, newBlockSuggestionsSet)
      }

      def allNewTxAndBlockSuggestions: F[(Set[Signed.Tx], Set[Block])] =
        LocalGossipService[F].get.map { localGossip =>
          (
            localGossip.newTxs.values.toSet,
            localGossip.newBlockSuggestions.values.map(_._1).toSet,
          )
        }

      def newTxAndBlockVotes(
          bloomFilter: BloomFilter,
          knownBlockVoteKeys: Set[(Block.BlockHash, Int)],
      ): F[(Set[Signed.Tx], Map[(Block.BlockHash, Int), Signature])] =
        LocalGossipService[F].get.map { localGossip =>
          val newTxsSet =
            localGossip.newTxs.filterNot(bloomFilter `check` _._1).values.toSet

          val newBlockVotesMap =
            localGossip.newBlockVotes -- knownBlockVoteKeys

          (newTxsSet, newBlockVotesMap)
        }

      def allNewTxAndBlockVotes(
          knownBlockVoteKeys: Set[(Block.BlockHash, Int)]
      ): F[(Set[Signed.Tx], Map[(Block.BlockHash, Int), Signature])] =
        LocalGossipService[F].get.map { localGossip =>
          (
            localGossip.newTxs.values.toSet,
            localGossip.newBlockVotes -- knownBlockVoteKeys,
          )
        }

      def txs(txHashes: Set[Signed.TxHash]): F[Set[Signed.Tx]] = txHashes.toList
        .traverse { (txHash) =>
          OptionT(
            LocalGossipService[F].get.map(_.newTxs.get(txHash))
          ).orElseF {
            TransactionRepository[F].get(txHash).getOrElseF {
              Effect[F].raiseError(
                new RuntimeException(s"Tx $txHash is failed to decode.")
              )
            }
          }.value
        }
        .map(_.flatten.toSet)

    }
}
