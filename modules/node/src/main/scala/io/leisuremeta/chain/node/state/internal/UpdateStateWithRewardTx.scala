package io.leisuremeta.chain
package node
package state
package internal

import java.time.{DayOfWeek, Instant, ZoneId, ZonedDateTime}
import java.time.temporal.{ChronoUnit, TemporalAdjusters}

import cats.Monoid
import cats.data.{EitherT, StateT}
import cats.effect.Concurrent
import cats.syntax.either.given
import cats.syntax.eq.given
import cats.syntax.foldable.given
import cats.syntax.traverse.given

import scodec.bits.BitVector

import GossipDomain.MerkleState
import UpdateState.*
import api.model.{
  Account,
  AccountSignature,
  GroupData,
  GroupId,
  PublicKeySummary,
  Signed,
  Transaction,
  TransactionWithResult,
}
import api.model.reward.*
import api.model.token.{NftState, TokenDefinitionId, TokenId}
import api.model.TransactionWithResult.ops.*
import lib.merkle.{MerkleTrie, MerkleTrieState}
import lib.codec.byte.{ByteDecoder, DecodeResult}
import lib.codec.byte.ByteEncoder.ops.*
import lib.crypto.Hash
import lib.crypto.Hash.ops.*
import lib.datatype.{BigNat, Utf8}
import repository.{BlockRepository, StateRepository, TransactionRepository}
import repository.StateRepository.given
import service.{RewardService, StateReadService}

trait UpdateStateWithRewardTx:

  given updateStateWithRewardTx[F[_]
    : Concurrent: BlockRepository: TransactionRepository: StateRepository.GroupState: StateRepository.TokenState: StateRepository.RewardState]
      : UpdateState[F, Transaction.RewardTx] =
    (ms: MerkleState, sig: AccountSignature, tx: Transaction.RewardTx) =>
      tx match
        case rd: Transaction.RewardTx.RegisterDao =>
          for
            groupDataOption <- MerkleTrie
              .get[F, GroupId, GroupData](rd.groupId.toBytes.bits)
              .runA(ms.group.groupState)
            groupData <- EitherT.fromOption[F](
              groupDataOption,
              s"Group does not exist: ${rd.groupId}",
            )
            daoInfoOption <- MerkleTrie
              .get[F, GroupId, DaoInfo](rd.groupId.toBytes.bits)
              .runA(ms.reward.daoState)
            _ <- {
              daoInfoOption match
                case Some(daoInfo) =>
                  EitherT.leftT(s"Group ${rd.groupId} exists already: $daoInfo")
                case None =>
                  EitherT.pure(())
            }
            daoInfo = DaoInfo(
              moderators = rd.moderators,
            )
            daoState <- MerkleTrie
              .put[F, GroupId, DaoInfo](rd.groupId.toBytes.bits, daoInfo)
              .runS(ms.reward.daoState)
          yield (
            ms.copy(
              reward = ms.reward.copy(
                daoState = daoState,
              ),
            ),
            TransactionWithResult(Signed(sig, rd), None),
          )
        case ud: Transaction.RewardTx.UpdateDao => ???
        case ra: Transaction.RewardTx.RecordActivity =>
          for
            userActivityState <- ra.userActivity.toList
              .traverse { case (account, activity) =>
                val canonicalInstant = ra.timestamp
                  .atZone(ZoneId.of("Asia/Seoul"))
                  .truncatedTo(ChronoUnit.DAYS)
                  .toInstant()
                val keyBits = (canonicalInstant, account).toBytes.bits
                for
                  activityOption <- MerkleTrie
                    .get[F, (Instant, Account), DaoActivity](keyBits)
                  _ <- MerkleTrie.remove[F, (Instant, Account), DaoActivity](
                    keyBits,
                  )
                  activity1 = activityOption.fold(activity)(
                    Monoid[DaoActivity].combine(_, activity),
                  )
                  _ <- MerkleTrie.put[F, (Instant, Account), DaoActivity](
                    keyBits,
                    activity1,
                  )
                yield ()
              }
              .runS(ms.reward.userActivityState)
            tokenReceivedState <- ra.tokenReceived.toList
              .traverse { case (tokenId, activity) =>
                MerkleTrie.put[F, (Instant, TokenId), DaoActivity](
                  (ra.timestamp, tokenId).toBytes.bits,
                  activity,
                )
              }
              .runS(ms.reward.tokenReceivedState)
          yield (
            ms.copy(reward =
              ms.reward.copy(
                userActivityState = userActivityState,
                tokenReceivedState = tokenReceivedState,
              ),
            ),
            TransactionWithResult(Signed(sig, ra), None),
          )
        case rgs: Transaction.RewardTx.RegisterStaking => ???
        case rms: Transaction.RewardTx.RemoveStaking   => ???
        case xr: Transaction.RewardTx.ExecuteReward =>
          val sourceAccount =
            xr.daoAccount.getOrElse(Account(Utf8.unsafeFrom("DAO-M")))
          val LM     = TokenDefinitionId(Utf8.unsafeFrom("LM"))
          val txHash = Hash[Transaction].apply(xr).toResultHashValue
          for
            balance <- getBalance[F](sourceAccount, LM, ms)
            (totalAmount, utxos) = balance
//            _ <- EitherT.pure { scribe.info(s"total amount: $totalAmount") }
            rewardCriterionInstant = RewardService.getLatestRewardInstantBefore(xr.createdAt)
            rewardCriterionState <- RewardService.findStateRootAt(rewardCriterionInstant)
            totalNumberOfDao <- RewardService.countDao[F](rewardCriterionState.reward.daoState)
            rarityItemMap    <- getRarityItem[F](rewardCriterionState.token)
//            _ <- EitherT.pure { scribe.info(s"rarityMap: $rarityItemMap") }
            outputs = calculateRarityReward(
              sourceAccount,
              totalAmount,
              totalNumberOfDao,
              rarityItemMap,
            )
            fungibleBalanceState <- updateBalanceWithReward(
              sourceAccount,
              LM,
              utxos,
              txHash,
              outputs,
              ms.token.fungibleBalanceState,
            )
          yield (
            ms.copy(
              token = ms.token.copy(
                fungibleBalanceState = fungibleBalanceState,
              ),
            ),
            TransactionWithResult(
              Signed(sig, xr),
              Some(Transaction.RewardTx.ExecuteRewardResult(outputs)),
            ),
          )

  def getBalance[F[_]
    : Concurrent: TransactionRepository: StateRepository.TokenState](
      account: Account,
      tokenDef: TokenDefinitionId,
      ms: MerkleState,
  ): EitherT[F, String, (BigNat, List[Hash.Value[TransactionWithResult]])] =
    val prefixBits = account.toBytes.bits ++ tokenDef.toBytes.bits
    for
      stream <- MerkleTrie
        .from[
          F,
          (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
          Unit,
        ](prefixBits)
        .runA(ms.token.fungibleBalanceState)
      balanceList <- stream
        .map(_._1)
        .takeWhile(_ `startsWith` prefixBits)
        .evalMap { keyBits =>
          for
            decodeResult <- EitherT.fromEither {
              ByteDecoder[
                (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
              ]
                .decode(keyBits.bytes)
                .leftMap(_.msg)
            }
            DecodeResult((_, _, txHash), remainder) = decodeResult
            _ <- EitherT.cond[F](
              remainder.isEmpty,
              (),
              s"Non-empty remainder: $remainder in nft-balance: ${keyBits.bytes}",
            )
            amount <- TransactionRepository[F]
              .get(txHash)
              .leftMap(_.msg)
              .flatMap {
                case Some(txWithResult) =>
                  txWithResult.signedTx.value match
                    case fb: Transaction.FungibleBalance =>
                      fb match
                        case mf: Transaction.TokenTx.MintFungibleToken =>
                          EitherT.pure(
                            mf.outputs.get(account).getOrElse(BigNat.Zero),
                          )
                        case bf: Transaction.TokenTx.BurnFungibleToken =>
                          txWithResult.result match
                            case Some(
                                  Transaction.TokenTx.BurnFungibleTokenResult(
                                    outputAmount,
                                  ),
                                ) =>
                              EitherT.pure(outputAmount)
                            case other =>
                              EitherT.leftT[F, BigNat](
                                s"burn fungible token result of $txHash has wrong result: $other",
                              )
                        case tf: Transaction.TokenTx.TransferFungibleToken =>
                          EitherT.pure(
                            tf.outputs.get(account).getOrElse(BigNat.Zero),
                          )

                        case ef: Transaction.TokenTx.EntrustFungibleToken =>
                          EitherT.pure(txWithResult.result.fold(BigNat.Zero) {
                            case Transaction.TokenTx
                                  .EntrustFungibleTokenResult(remainder) =>
                              remainder
                            case _ => BigNat.Zero
                          })
                        case df: Transaction.TokenTx.DisposeEntrustedFungibleToken =>
                          EitherT.pure(
                            df.outputs.get(account).getOrElse(BigNat.Zero),
                          )
                        case xr: Transaction.RewardTx.ExecuteReward =>
                          EitherT.pure {
                            txWithResult.result match
                              case Some(
                                    Transaction.RewardTx.ExecuteRewardResult(
                                      outputs,
                                    ),
                                  ) =>
                                outputs.get(account).getOrElse(BigNat.Zero)
                              case _ => BigNat.Zero
                          }
                    case _ =>
                      EitherT.leftT[F, BigNat](
                        s"input tx $txHash is not a fungible balance",
                      )
                case None =>
                  EitherT.leftT[F, BigNat](s"input tx $txHash does not exist")
              }
          yield (amount, txHash)
        }
        .compile
        .toList
    yield
      val (amountList, txHashList) = balanceList.unzip
      (amountList.foldLeft(BigNat.Zero)(_ + _), txHashList)

  def getRarityItem[F[_]
    : Concurrent: StateRepository.TokenState: StateRepository.RewardState](
      state: GossipDomain.MerkleState.TokenMerkleState,
  ): EitherT[F, String, Map[Account, BigNat]] =
    for
      stream <- MerkleTrie
        .from[F, (Account, TokenId, Hash.Value[TransactionWithResult]), Unit](
          BitVector.empty,
        )
        .runA(state.nftBalanceState)
      result <- stream
        .evalMap { (keyBits, _) =>
          for
            decodeResult <- EitherT.fromEither {
              ByteDecoder[
                (Account, TokenId, Hash.Value[TransactionWithResult]),
              ]
                .decode(keyBits.bytes)
                .leftMap(_.msg)
            }
            DecodeResult((account, tokenId, _), remainder) = decodeResult
            _ <- EitherT.cond[F](
              remainder.isEmpty,
              (),
              s"Non-empty remainder: $remainder in nft-balance: ${keyBits.bytes}",
            )
            nftStateOption <- MerkleTrie
              .get[F, TokenId, NftState](tokenId.toBytes.bits)
              .runA(state.nftState)
            nftState <- EitherT.fromOption(
              nftStateOption,
              s"Nft state not found: $tokenId",
            )
          yield (account, nftState.weight)
        }
        .fold(Map.empty[Account, BigNat]) { case (map, (account, weight)) =>
          val weight1 = map.get(account).fold(weight)(_ + weight)
          map + (account -> weight1)
        }
        .compile
        .toList
    yield result.head

  def calculateRarityReward(
      sourceAccount: Account,
      totalRarityRewardAmount: BigNat,
      totalNumberOfDao: Int,
      rarityRewardPoint: Map[Account, BigNat],
  ): Map[Account, BigNat] =
    val e18 = BigInt(10).pow(18)
    val limit =
      BigNat.unsafeFromBigInt(BigInt(250_000L) * e18 * totalNumberOfDao)
    val totalAmount = BigNat.min(totalRarityRewardAmount, limit)
    val pointSum    = rarityRewardPoint.values.foldLeft(BigNat.Zero)(_ + _)

    val rewardMap = rarityRewardPoint.view.mapValues { point =>
      (totalAmount * point / pointSum).floorAt(16)
    }.toMap
    val rewardSum = rewardMap.values.foldLeft(BigNat.Zero)(_ + _)
    val remainder = BigNat.unsafeFromBigInt{
      totalRarityRewardAmount.toBigInt - rewardSum.toBigInt
    } 

    rewardMap + (sourceAccount -> remainder)

  type BalanceKey =
    (Account, TokenDefinitionId, Hash.Value[TransactionWithResult])

  def updateBalanceWithReward[F[_]: cats.Monad: StateRepository.TokenState](
      sourceAccount: Account,
      tokenDef: TokenDefinitionId,
      utxos: List[Hash.Value[TransactionWithResult]],
      txHash: Hash.Value[TransactionWithResult],
      outputs: Map[Account, BigNat],
      balanceState: MerkleTrieState[BalanceKey, Unit],
  ): EitherT[F, String, MerkleTrieState[BalanceKey, Unit]] =
    val program = for
      _ <- utxos.traverse { utxo =>
        MerkleTrie.remove[F, BalanceKey, Unit](
          (sourceAccount, tokenDef, utxo).toBytes.bits,
        )
      }
      _ <- outputs.toList.traverse { case (account, amount) =>
        MerkleTrie.put[F, BalanceKey, Unit](
          (account, tokenDef, txHash).toBytes.bits,
          (),
        )
      }
    yield ()

    program.runS(balanceState)

end UpdateStateWithRewardTx
