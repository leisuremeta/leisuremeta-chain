package io.leisuremeta.chain
package node
package dapp
package submodule

import cats.data.{EitherT, StateT}
import cats.effect.Concurrent
import cats.syntax.traverse.*

import api.model.{
  AccountSignature,
  Signed,
  Transaction,
  TransactionWithResult,
}
import api.model.reward.{
  ActivityLog,
  DaoInfo,
}
import api.model.TransactionWithResult.ops.*
import lib.crypto.Hash
import lib.crypto.Hash.ops.*
import lib.datatype.BigNat
import lib.merkle.MerkleTrieState
import repository.TransactionRepository

object PlayNommDAppReward:
  def apply[F[_]: Concurrent: TransactionRepository: PlayNommState](
      tx: Transaction.RewardTx,
      sig: AccountSignature,
  ): StateT[
    EitherT[F, PlayNommDAppFailure, *],
    MerkleTrieState,
    TransactionWithResult,
  ] = tx match
    case rd: Transaction.RewardTx.RegisterDao =>
      for
        groupDataOption <- PlayNommState[F].group.group
          .get(rd.groupId)
          .mapK:
            PlayNommDAppFailure.mapInternal(s"Fail to get group ${rd.groupId}")
        _ <- checkExternal(
          groupDataOption.isDefined,
          s"Group ${rd.groupId} not found",
        )
        daoInfoOption <- PlayNommState[F].reward.dao
          .get(rd.groupId)
          .mapK:
            PlayNommDAppFailure.mapInternal(s"Fail to get group ${rd.groupId}")
        _ <- checkExternal(
          daoInfoOption.isEmpty,
          s"Group ${rd.groupId} already has DAO",
        )
        daoInfo = DaoInfo(moderators = rd.moderators)
        _ <- PlayNommState[F].reward.dao
          .put(rd.groupId, daoInfo)
          .mapK:
            PlayNommDAppFailure.mapInternal(s"Fail to put DAO ${rd.groupId}")
      yield TransactionWithResult(Signed(sig, tx), None)

    case or: Transaction.RewardTx.OfferReward =>
      for
        _        <- PlayNommDAppAccount.verifySignature(sig, tx)
        tokenDef <- PlayNommDAppToken.getTokenDefinition(or.tokenDefinitionId)
        inputAmount <- PlayNommDAppToken.getFungibleBalanceTotalAmounts(
          or.inputs.map(_.toResultHashValue),
          sig.account,
        )
        outputAmount = or.outputs.values.foldLeft(BigNat.Zero)(BigNat.add)
        diff <- fromEitherExternal:
          BigNat.tryToSubtract(inputAmount, outputAmount)
        txWithResult = TransactionWithResult(Signed(sig, or))(None)
        txHash       = txWithResult.toHash
        _ <- PlayNommDAppToken.removeInputUtxos(
          sig.account,
          or.inputs.map(_.toResultHashValue),
          or.tokenDefinitionId,
        )
        _ <- or.outputs.toSeq.traverse:
          case (account, _) =>
            PlayNommDAppToken.putBalance(account, or.tokenDefinitionId, txHash)
        totalAmount <- fromEitherInternal:
          BigNat.tryToSubtract(tokenDef.totalAmount, diff)
        _ <- PlayNommDAppToken.putTokenDefinition(
          or.tokenDefinitionId,
          tokenDef.copy(totalAmount = totalAmount),
        )
      yield txWithResult

    case tx: Transaction.RewardTx.UpdateDao => ???
    case tx: Transaction.RewardTx.RecordActivity =>
      val txResult = TransactionWithResult(Signed(sig, tx), None)
      val txHash   = txResult.toHash
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

    case tx: Transaction.RewardTx.ExecuteReward => ???
    case tx: Transaction.RewardTx.BuildSnapshot => ???
//      val getNftStateStream =
//        StateT
//          .inspectF { (ms: MerkleState) =>
//            GenericMerkleTrie
//              .from[F, TokenId, NftState](BitVector.empty)
//              .runA(ms.token.nftState)
//              .map { stream =>
//                stream.evalMap { case (_, nftState) =>
//                  GenericMerkleTrie
//                    .from[
//                      F,
//                      (Account, TokenId, Hash.Value[TransactionWithResult]),
//                      Unit,
//                    ] {
//                      (nftState.currentOwner, nftState.tokenId).toBytes.bits
//                    }
//                    .runA(ms.token.nftBalanceState)
//                    .map { stream =>
//                      stream
//                        .evalMap { case (keyBits, _) =>
//                          for
//                            txHash <- EitherT.fromEither {
//                              keyBits.bytes
//                                .to[
//                                  (
//                                      Account,
//                                      TokenId,
//                                      Hash.Value[TransactionWithResult],
//                                  ),
//                                ]
//                                .map(_._3)
//                                .leftMap(_.msg)
//                            }
//                            txOption <- TransactionRepository[F]
//                              .get(txHash)
//                              .leftMap(_.msg)
//                            tx <- EitherT.fromOption(
//                              txOption,
//                              s"No transaction $txHash found",
//                            )
//                          yield tx
//                        }
//                        .filter(
//                          _.signedTx.value.createdAt.compareTo(tx.timestamp) < 0,
//                        )
//                        .as(nftState)
//                    }
//                }.flatten
//              }
//          }
//          .mapK(PlayNommDAppFailure.mapInternal(s"Fail to get NFT states"))
//
//      val getWeightSum = getNftStateStream.flatMapF { stream =>
//        stream
//          .map(_.weight)
//          .fold(BigNat.Zero)(BigNat.add)
//          .compile
//          .toList
//          .flatMap { list => EitherT.fromOption(list.headOption, "empty list") }
//          .leftMap { e =>
//            PlayNommDAppFailure.internal(s"Fail to get weight sum: $e")
//          }
//      }
//
//      for
//        nftStream <- getNftStateStream
//        weightSum <- getWeightSum
//        _ <- StateT.modifyF[EitherT[F, PlayNommDAppFailure, *], MerkleState] {
//          (ms: MerkleState) =>
//            nftStream
//              .evalScan(ms) { (ms, nftState) =>
//                val ownershipSnapshot = OwnershipSnapshot(
//                  account = nftState.currentOwner,
//                  timestamp = tx.timestamp,
//                  point = nftState.weight,
//                  definitionId = nftState.tokenDefinitionId,
//                  amount = tx.ownershipAmount * nftState.weight / weightSum,
//                )
//                PlayNommState[F].reward.ownershipSnapshot
//                  .put(nftState.tokenId, ownershipSnapshot)
//                  .transformS[MerkleState](
//                    _.main,
//                    (ms, mts) => (ms.copy(main = mts)),
//                  )
//                  .runS(ms)
//              }
//              .last
//              .compile
//              .toList
//              .flatMap { list =>
//                EitherT.fromOption(
//                  list.headOption.flatten,
//                  "Fail to build final mekle state",
//                )
//              }
//              .leftMap(e => PlayNommDAppFailure.internal(e))
//        }
//      yield TransactionWithResult(Signed(sig, tx), None)

    case tx: Transaction.RewardTx.ExecuteOwnershipReward => ???
//      val getInputAmount
//          : StateT[EitherT[F, PlayNommDAppFailure, *], MerkleState, BigNat] =
//        StateT.liftF {
//          tx.inputs.toList
//            .traverse { txHash =>
//              for
//                txOption <- TransactionRepository[F].get(txHash).leftMap { e =>
//                  PlayNommDAppFailure.internal(s"fail to get tx $txHash")
//                }
//                txWithResult <- EitherT.fromOption(
//                  txOption,
//                  PlayNommDAppFailure.external(s"Tx input not found: $txHash"),
//                )
//                amount <- txWithResult.signedTx.value match
//                  case fb: Transaction.FungibleBalance =>
//                    EitherT.pure {
//                      fb match
//                        case mf: Transaction.TokenTx.MintFungibleToken =>
//                          mf.outputs.get(sig.account).getOrElse(BigNat.Zero)
//                        case or: Transaction.TokenTx.TransferFungibleToken =>
//                          or.outputs.get(sig.account).getOrElse(BigNat.Zero)
//                        case bf: Transaction.TokenTx.BurnFungibleToken =>
//                          txWithResult.result.fold(BigNat.Zero) {
//                            case Transaction.TokenTx.BurnFungibleTokenResult(
//                                  outputAmount,
//                                ) =>
//                              outputAmount
//                            case _ => BigNat.Zero
//                          }
//                        case ef: Transaction.TokenTx.EntrustFungibleToken =>
//                          txWithResult.result.fold(BigNat.Zero) {
//                            case Transaction.TokenTx.EntrustFungibleTokenResult(
//                                  remainder,
//                                ) =>
//                              remainder
//                            case _ => BigNat.Zero
//                          }
//                        case de: Transaction.TokenTx.DisposeEntrustedFungibleToken =>
//                          de.outputs.get(sig.account).getOrElse(BigNat.Zero)
//                        case or: Transaction.RewardTx.OfferReward =>
//                          or.outputs.get(sig.account).getOrElse(BigNat.Zero)
//                        case xr: Transaction.RewardTx.ExecuteReward =>
//                          txWithResult.result.fold(BigNat.Zero) {
//                            case Transaction.RewardTx.ExecuteRewardResult(
//                                  outputs,
//                                ) =>
//                              outputs.get(sig.account).getOrElse(BigNat.Zero)
//                            case _ => BigNat.Zero
//                          }
//                        case xo: Transaction.RewardTx.ExecuteOwnershipReward =>
//                          txWithResult.result.fold(BigNat.Zero) {
//                            case Transaction.RewardTx
//                                  .ExecuteOwnershipRewardResult(
//                                    outputs,
//                                  ) =>
//                              outputs.get(sig.account).getOrElse(BigNat.Zero)
//                            case _ => BigNat.Zero
//                          }
//                    }
//                  case _ =>
//                    EitherT.leftT(PlayNommDAppFailure.external {
//                      s"Tx input is not a fungible balance: $txHash"
//                    })
//              yield amount
//            }
//            .map(_.foldLeft(BigNat.Zero)(BigNat.add))
//        }
//
//      def updateBalance(
//          outputs: Map[Account, BigNat],
//          outputTxHash: Hash.Value[TransactionWithResult],
//      ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleState, Unit] =
//        val program =
//          for
//            _ <- tx.inputs.toList.traverse { inputTxHash =>
//              GenericMerkleTrie
//                .remove[
//                  F,
//                  (
//                      Account,
//                      TokenDefinitionId,
//                      Hash.Value[TransactionWithResult],
//                  ),
//                  Unit,
//                ] {
//                  (sig.account, tx.definitionId, inputTxHash).toBytes.bits
//                }
//            }
//            _ <- outputs.toList.traverse { (account, amount) =>
//              GenericMerkleTrie.put[
//                F,
//                (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
//                Unit,
//              ](
//                (account, tx.definitionId, outputTxHash).toBytes.bits,
//                (),
//              )
//            }
//          yield ()
//
//        program
//          .transformS[MerkleState](
//            _.token.fungibleBalanceState,
//            (ms, fbs) =>
//              (ms.copy(token = ms.token.copy(fungibleBalanceState = fbs))),
//          )
//          .mapK(
//            PlayNommDAppFailure.mapInternal("Fail to update fungible balance"),
//          )
//
//      def updateRewarded(txHash: Hash.Value[TransactionWithResult])(
//          ownershipSnapshot: (TokenId, OwnershipSnapshot),
//      ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleState, Unit] =
//        val program =
//          for _ <- PlayNommState[F].reward.ownershipRewarded.put(
//              ownershipSnapshot._1,
//              OwnershipRewardLog(ownershipSnapshot._2, txHash),
//            )
//          yield ()
//
//        program
//          .mapK(
//            PlayNommDAppFailure.mapInternal("Fail to update rewarded state"),
//          )
//          .transformS[MerkleState](_.main, (ms, mts) => (ms.copy(main = mts)))
//
//      for
//        snapshots <- tx.targets.toList
//          .traverse { tokenId =>
//            for
//              ownershipSnapshotOption <-
//                PlayNommState[F].reward.ownershipSnapshot
//                  .get(tokenId)
//                  .mapK(
//                    PlayNommDAppFailure.mapInternal(
//                      s"Fail to get snapshot data of token ${tokenId}",
//                    ),
//                  )
//              ownershipSnapshot <- fromOption(
//                ownershipSnapshotOption,
//                s"No ownership snapshot for token $tokenId",
//              )
//            yield ownershipSnapshot
//          }
//          .transformS[MerkleState](_.main, (ms, mts) => (ms.copy(main = mts)))
//        inputAmount <- getInputAmount
//        outputSum = snapshots.map(_.amount).foldLeft(BigNat.Zero)(BigNat.add)
//
//        remainder <- StateT.liftF {
//          EitherT
//            .fromEither {
//              BigNat.fromBigInt(outputSum.toBigInt - inputAmount.toBigInt)
//            }
//            .leftMap(PlayNommDAppFailure.external)
//        }
//        outputs = snapshots.map { snapshot =>
//          snapshot.account -> snapshot.amount
//        }.toMap + (sig.account -> remainder)
//        result = Transaction.RewardTx.ExecuteOwnershipRewardResult(outputs)
//        txWithResult = TransactionWithResult(Signed(sig, tx), Some(result))
//        txHash       = txWithResult.toHash
//        _ <- updateBalance(outputs, txHash)
//        _ <- (tx.targets.toList zip snapshots).traverse(updateRewarded(txHash))
//      yield txWithResult
