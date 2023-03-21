package io.leisuremeta.chain
package node
package dapp
package submodule

import cats.data.{EitherT, StateT}
import cats.effect.Concurrent
import cats.syntax.either.*
import cats.syntax.traverse.*

import fs2.Stream
import scodec.bits.BitVector

import api.model.{Account, AccountSignature, Signed, Transaction, TransactionWithResult}
import api.model.reward.{ActivityLog, OwnershipSnapshot}
import api.model.token.{NftState, TokenId}
import lib.codec.byte.ByteDecoder.ops.*
import lib.codec.byte.ByteEncoder.ops.*
import lib.crypto.Hash
import lib.crypto.Hash.ops.*
import lib.datatype.BigNat
import lib.merkle.{GenericMerkleTrie, MerkleTrieState}
import repository.{GenericStateRepository, TransactionRepository}
import repository.GenericStateRepository.given

import GossipDomain.MerkleState
import PlayNommDAppUtil.*
import io.leisuremeta.chain.api.model.reward.OwnershipRewardLog

object PlayNommDAppReward:
  def apply[F[_]
    : Concurrent: TransactionRepository: PlayNommState: GenericStateRepository.TokenState](
      tx: Transaction.RewardTx,
      sig: AccountSignature,
  ): StateT[
    EitherT[F, PlayNommDAppFailure, *],
    MerkleState,
    TransactionWithResult,
  ] = tx match
    case tx: Transaction.RewardTx.RegisterDao => ???
    case tx: Transaction.RewardTx.UpdateDao   => ???
    case tx: Transaction.RewardTx.RecordActivity =>
      val txResult = TransactionWithResult(Signed(sig, tx), None)
      val txHash   = txResult.toHash
      val program: StateT[
        EitherT[F, PlayNommDAppFailure, *],
        MerkleTrieState,
        TransactionWithResult,
      ] =
        for
          _ <- tx.userActivity.toList.traverse { case (account, activities) =>
            val logs = activities.map { a =>
              ActivityLog(a.point, a.description, txHash)
            }
            PlayNommState[F].reward.accountActivity
              .put((account, tx.timestamp), logs)
              .mapK {
                PlayNommDAppFailure.mapInternal {
                  s"Fail to put account activity in $txHash"
                }
              }
          }
          _ <- tx.tokenReceived.toList.traverse { case (account, activities) =>
            val logs = activities.map { a =>
              ActivityLog(a.point, a.description, txHash)
            }
            PlayNommState[F].reward.tokenReceived
              .put((account, tx.timestamp), logs)
              .mapK {
                PlayNommDAppFailure.mapInternal {
                  s"Fail to put account activity in $txHash"
                }
              }
          }
        yield txResult

      program
        .transformS[MerkleState](_.main, (ms, mts) => (ms.copy(main = mts)))

    case tx: Transaction.RewardTx.OfferReward   => ???
    case tx: Transaction.RewardTx.ExecuteReward => ???
    case tx: Transaction.RewardTx.BuildSnapshot =>
      val getNftStateStream =
        StateT.inspectF{ (ms: MerkleState) =>
          GenericMerkleTrie
            .from[F, TokenId, NftState](BitVector.empty)
            .runA(ms.token.nftState)
            .map { stream =>
              stream.evalMap{ case (_, nftState) =>
                GenericMerkleTrie
                  .from[F, (Account, TokenId, Hash.Value[TransactionWithResult]), Unit]{
                    (nftState.currentOwner, nftState.tokenId).toBytes.bits
                  }
                  .runA(ms.token.nftBalanceState)
                  .map{ stream =>
                    stream
                      .evalMap{ case (keyBits, _) =>
                        for
                          txHash <- EitherT.fromEither{
                            keyBits.bytes
                              .to[(Account, TokenId, Hash.Value[TransactionWithResult])]
                              .map(_._3)
                              .leftMap(_.msg)
                          }
                          txOption <- TransactionRepository[F].get(txHash).leftMap(_.msg)
                          tx <- EitherT.fromOption(txOption, s"No transaction $txHash found")
                        yield tx
                      }
                      .filter(_.signedTx.value.createdAt.compareTo(tx.timestamp) < 0)
                      .as(nftState)
                  }
              }.flatten
            }
        }
        .mapK(PlayNommDAppFailure.mapInternal(s"Fail to get NFT states"))

      val getWeightSum = getNftStateStream.flatMapF { stream =>
        stream
          .map(_.weight)
          .fold(BigNat.Zero)(BigNat.add)
          .compile
          .toList
          .flatMap { list => EitherT.fromOption(list.headOption, "empty list") }
          .leftMap { e =>
            PlayNommDAppFailure.internal(s"Fail to get weight sum: $e")
          }
      }

      for
        nftStream <- getNftStateStream
        weightSum <- getWeightSum
        _ <- StateT.modifyF[EitherT[F, PlayNommDAppFailure, *], MerkleState] {
          (ms: MerkleState) =>
            nftStream
              .evalScan(ms) { (ms, nftState) =>
                val ownershipSnapshot = OwnershipSnapshot(
                  account = nftState.currentOwner,
                  timestamp = tx.timestamp,
                  point = nftState.weight,
                  definitionId = nftState.tokenDefinitionId,
                  amount = tx.ownershipAmount * nftState.weight / weightSum,
                )
                PlayNommState[F].reward.ownershipSnapshot
                  .put(nftState.tokenId, ownershipSnapshot)
                  .transformS[MerkleState](
                    _.main,
                    (ms, mts) => (ms.copy(main = mts)),
                  )
                  .runS(ms)
              }
              .last
              .compile
              .toList
              .flatMap { list =>
                EitherT.fromOption(
                  list.headOption.flatten,
                  "Fail to build final mekle state",
                )
              }
              .leftMap(e => PlayNommDAppFailure.internal(e))
        }
      yield TransactionWithResult(Signed(sig, tx), None)

    case tx: Transaction.RewardTx.ExecuteOwnershipReward =>

      ???
