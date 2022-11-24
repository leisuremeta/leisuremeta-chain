package io.leisuremeta.chain
package node
package service

import java.time.{DayOfWeek, Instant, ZoneId, ZonedDateTime}
import java.time.temporal.{ChronoUnit, TemporalAdjusters}

import cats.{Monad, Monoid}
import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.either.catsSyntaxEither
import cats.syntax.eq.catsSyntaxEq
import cats.syntax.foldable.toFoldableOps
import cats.syntax.functor.toFunctorOps
import cats.syntax.traverse.toTraverseOps

import fs2.Stream
import scodec.bits.BitVector

import GossipDomain.MerkleState
import api.model.{Account, Block, GroupId, StateRoot, TransactionWithResult}
import api.model.Block.ops.*
import api.model.TransactionWithResult.ops.*
import api.model.api_model.RewardInfo
import api.model.reward.DaoInfo
import api.model.token.{
  NftState,
  Rarity,
  TokenDefinition,
  TokenDefinitionId,
  TokenId,
}
import lib.codec.byte.{ByteDecoder, DecodeResult}
import lib.codec.byte.ByteEncoder.ops.*
import lib.crypto.Hash
import lib.crypto.Hash.ops.*
import lib.datatype.{BigNat, Utf8}
import lib.merkle.{MerkleTrie, MerkleTrieState}
import lib.merkle.MerkleTrie.NodeStore
import repository.{BlockRepository, StateRepository, TransactionRepository}
import repository.StateRepository.given

import lib.merkle.{MerkleTrie, MerkleTrieState}
import io.leisuremeta.chain.api.model.reward.DaoActivity

object RewardService:

  def getRewardInfoFromBestHeader[F[_]
    : Concurrent: BlockRepository: TransactionRepository: StateRepository.RewardState: StateRepository.TokenState](
      account: Account,
      timestampOption: Option[Instant],
      daoAccount: Option[Account],
      rewardAmount: Option[BigNat],
  ): EitherT[F, String, RewardInfo] =
    for
      bestHeaderOption <- BlockRepository[F].bestHeader.leftMap(_.msg)
      bestHeader <- EitherT.fromOption(
        bestHeaderOption,
        s"Best header not found",
      )
      stateRoot = GossipDomain.MerkleState.from(bestHeader)
      info <- getRewardInfo[F](
        account,
        timestampOption,
        daoAccount,
        rewardAmount,
        stateRoot.reward,
        stateRoot.token,
      )
    yield info

  def getRewardInfo[F[_]
    : Concurrent: BlockRepository: TransactionRepository: StateRepository.RewardState: StateRepository.TokenState](
      account: Account,
      timestampOption: Option[Instant],
      daoAccount: Option[Account],
      rewardAmount: Option[BigNat],
      rewardMerkleState: GossipDomain.MerkleState.RewardMerkleState,
      tokenMerkleState: GossipDomain.MerkleState.TokenMerkleState,
  ): EitherT[F, String, RewardInfo] =
    val timestamp          = timestampOption.getOrElse(Instant.now())
    val canonicalTimestamp = getLatestRewardInstantBefore(timestamp)
    val userActivityState  = rewardMerkleState.userActivityState
    val tokenReceivedState = rewardMerkleState.tokenReceivedState

    for
      totalNumberOfDao <- countDao[F](rewardMerkleState.daoState)
//      _ <- EitherT.pure(scribe.info(s"Total number of DAO: ${totalNumberOfDao}"))
      userActivities <- getWeeklyUserActivities[F](
        timestamp,
        account,
        userActivityState,
      )
      userActivityMilliPoint = userActivities.flatten
        .map(dailyDaoActivityToMilliPoint)
        .foldLeft(BigNat.Zero)(BigNat.add)
//      _ <- EitherT.pure(scribe.info(s"userActivities: ${userActivities.flatten}"))
      totalActivityMilliPoint <- getWeeklyTotalActivityPoint[F](
        timestamp,
        userActivityState,
      )
//      _ <- EitherT.pure(scribe.info(s"totalActivityMilliPoint: ${totalActivityMilliPoint}"))
      userActivityReward = calculateUserActivityReward(
        totalNumberOfDao,
        userActivityMilliPoint,
        totalActivityMilliPoint,
      )
      stateRoot <- findStateRootAt(canonicalTimestamp)
//      _ <- EitherT.pure(scribe.info(s"stateRoot: ${stateRoot}"))
      tokens <- getNftOwned(account, stateRoot.token.nftBalanceState)
//      _ <- EitherT.pure(scribe.info(s"tokens: ${tokens}"))
      tokenReceivedDaoActivity <- getWeeklyTokenReceived(
        tokens,
        canonicalTimestamp,
        tokenReceivedState,
      )
//      _ <- EitherT.pure(scribe.info(s"tokenReceivedDaoActivity: ${tokenReceivedDaoActivity}"))
      totalReceivedMilliPoint <- getWeeklyTotalReceivedPoint(
        timestamp,
        tokenReceivedState,
      )
//      _ <- EitherT.pure(scribe.info(s"totalReceivedMilliPoint: ${totalReceivedMilliPoint}"))
      tokenReceivedReward = calculateTokenReceivedReward(
        totalNumberOfDao,
        tokenReceivedDaoActivity,
        totalReceivedMilliPoint,
      )
      totalRarityRewardAmount <- getTotalRarityRewardAmount(
        rewardAmount,
        daoAccount,
      )
      userRarityRewardItems <- getUserRarityItem(account, tokenMerkleState)
//      _ <- EitherT.pure(scribe.info(s"userRarityRewardItems: ${userRarityRewardItems}"))
      userRarityReward = rarityItemsToRewardDetailMap(userRarityRewardItems)
      totalRarityRewardValue <- getTotalRarityRewardValue(
        account,
        tokenMerkleState,
      )
      _ <- EitherT.pure(
        scribe.info(s"totalRarityRewardValue: ${totalRarityRewardValue}"),
      )
      userRarityRewardValue = calculateUserRarityRewardValue(
        totalRarityRewardAmount,
        totalNumberOfDao,
        userRarityRewardItems,
        totalRarityRewardValue,
      )
      isModerator <- isModerator(account, rewardMerkleState.daoState)
      bonus =
        if isModerator then userActivityReward + userActivityReward
        else BigNat.Zero
      total =
        userActivityReward + tokenReceivedReward + userRarityRewardValue + bonus
    yield RewardInfo(
      account = account,
      reward = RewardInfo.Reward(
        total = total,
        activity = userActivityReward,
        token = tokenReceivedReward,
        rarity = userRarityRewardValue,
        bonus = bonus,
      ),
      point = RewardInfo.Point(
        activity = userActivities.flatten.combineAll,
        token = tokenReceivedDaoActivity,
        rarity = userRarityReward,
      ),
      timestamp = timestamp,
      totalNumberOfDao = BigNat.unsafeFromLong(totalNumberOfDao),
    )

  /** count dao (max value 100)
    *
    * @return
    *   number of dao (if larger than 100, return 100)
    */
  def countDao[F[_]: Concurrent: StateRepository.RewardState](
      daoState: MerkleTrieState[GroupId, DaoInfo],
  ): EitherT[F, String, Int] =
    for
      stream <- MerkleTrie
        .from[F, GroupId, DaoInfo](BitVector.empty)
        .runA(daoState)
      size <- stream.take(100).compile.count
    yield size.toInt

  def calculateUserActivityReward(
      numberOfDao: Int,
      userActivityMilliPoint: BigNat,
      totalActivityMilliPoint: BigNat,
  ): BigNat =
    val limit = BigInt(120_000L) * 1000 * numberOfDao
    val milliPoint: BigNat =
      if totalActivityMilliPoint.toBigInt <= limit then userActivityMilliPoint
      else
        BigNat.unsafeFromBigInt(
          limit,
        ) * userActivityMilliPoint / totalActivityMilliPoint
    milliPoint * BigNat.unsafeFromBigInt(BigInt(10).pow(15))

  def getWeeklyUserActivities[F[_]: Concurrent: StateRepository.RewardState](
      timestamp: Instant,
      user: Account,
      root: MerkleTrieState[(Instant, Account), DaoActivity],
  ): EitherT[F, String, Seq[Option[DaoActivity]]] =
    val refInstants = getWeeklyRefTime(
      getLatestRewardInstantBefore(timestamp),
    )

    refInstants
      .traverse { (refInstant) =>
        MerkleTrie.get[F, (Instant, Account), DaoActivity](
          (refInstant, user).toBytes.bits,
        )
      }
      .runA(root)

  def getLatestRewardInstantBefore(timestamp: Instant): Instant =
    timestamp
      .atZone(ZoneId.of("Asia/Seoul"))
      .`with`(TemporalAdjusters.previous(DayOfWeek.MONDAY))
      .truncatedTo(ChronoUnit.DAYS)
      .toInstant()

  def getWeeklyRefTime(last: Instant): Seq[Instant] =
    Seq.tabulate(7)(i => last.minus(7 - i, ChronoUnit.DAYS))

  def dailyDaoActivityToMilliPoint(a: DaoActivity): BigNat =
    BigNat
      .fromBigInt {
        daoActivityToMilliPoint(a).max(4000)
      }
      .toOption
      .getOrElse(BigNat.Zero)

  def daoActivityToMilliPoint(a: DaoActivity): BigInt =
    val weights = Seq(10, 10, 10, -10)
    Seq(a.like, a.comment, a.share, a.report)
      .map(_.toBigInt)
      .zip(weights)
      .map { case (number, weight) => number * weight }
      .sum

  def getWeeklyTotalActivityPoint[F[_]
    : Concurrent: StateRepository.RewardState](
      timestamp: Instant,
      root: MerkleTrieState[(Instant, Account), DaoActivity],
  ): EitherT[F, String, BigNat] =
    val to: Instant   = getLatestRewardInstantBefore(timestamp)
    val from: Instant = getLatestRewardInstantBefore(to)
    val timestampBits = from.toBytes.bits

    MerkleTrie
      .from[F, (Instant, Account), DaoActivity](timestampBits)
      .runA(root)
      .flatMap { stream =>
        stream
          .evalMap { case (keyBits, daoActivity) =>
            EitherT.fromEither[F] {
              ByteDecoder[(Instant, Account)]
                .decode(keyBits.bytes)
                .leftMap(_.msg)
                .flatMap { case DecodeResult((instant, account), remainder) =>
                  if remainder.isEmpty then Right((instant, daoActivity))
                  else
                    Left(
                      s"Non-empty remainder: $remainder in account: ${keyBits.bytes}",
                    )
                },
            }
          }
          .takeWhile(_._1.compareTo(to) <= 0)
          .map(_._2)
          .map(dailyDaoActivityToMilliPoint)
          .fold(BigNat.Zero)(BigNat.add)
          .compile
          .toList
      }
      .map(_.head)

  def calculateWeeklyUserActivityReward(
      weeklyUserActivity: Seq[DaoActivity],
  ): BigNat =

    val weights = Seq(10, 10, 10, -10)

    val sumBigInt = weeklyUserActivity.map { (a: DaoActivity) =>
      Seq(a.like, a.comment, a.share, a.report)
        .map(_.toBigInt)
        .zip(weights)
        .map { case (number, weight) => number * weight }
        .sum
        .max(4000) // Max daily millipoint
    }.sum

    BigNat.fromBigInt(sumBigInt).toOption.getOrElse(BigNat.Zero)

  def getUserActivityTimeWindows(last: Instant): Seq[Instant] =
    Seq.tabulate(8)(i => last.minus(7 - i, ChronoUnit.DAYS))

  def findStateRootAt[F[_]: Monad: BlockRepository: TransactionRepository](
      instant: Instant,
  ): EitherT[F, String, GossipDomain.MerkleState] =

    def loop(
        blockHash: Block.BlockHash,
    ): EitherT[F, String, GossipDomain.MerkleState] =
      for
        blockOption <- BlockRepository[F].get(blockHash).leftMap(_.msg)
        block <- EitherT.fromOption(blockOption, s"Block not found: $blockHash")
        txs <- block.transactionHashes.toList.traverse { txHash =>
          TransactionRepository[F]
            .get(txHash.toResultHashValue)
            .leftMap(_.msg)
            .flatMap(
              EitherT.fromOption(
                _,
                s"Transaction $txHash is not found in block $blockHash",
              ),
            )
        }
        ans <-
          if txs.exists(_.signedTx.value.createdAt.compareTo(instant) > 0) then
            loop(block.header.parentHash)
          else EitherT.pure(MerkleState.from(block.header))
      yield ans

    BlockRepository[F].bestHeader
      .leftMap(_.msg)
      .map(_.get.toHash.toBlockHash)
      .flatMap(loop)

  def getNftOwned[F[_]: Concurrent: StateRepository.TokenState](
      user: Account,
      state: MerkleTrieState[
        (Account, TokenId, Hash.Value[TransactionWithResult]),
        Unit,
      ],
  ): EitherT[F, String, List[TokenId]] =
    for
      stream <- MerkleTrie
        .from[F, (Account, TokenId, Hash.Value[TransactionWithResult]), Unit](
          user.toBytes.bits,
        )
        .runA(state)
      tokenIds <- stream
        .evalMap { case (keyBits, ()) =>
          EitherT.fromEither[F] {
            ByteDecoder[(Account, TokenId, Hash.Value[TransactionWithResult])]
              .decode(keyBits.bytes)
              .leftMap(_.msg)
              .flatMap {
                case DecodeResult((account, tokenId, txHash), remainder)
                    if remainder.isEmpty =>
                  Right((account, tokenId))
                case _ =>
                  Left(
                    s"fail to decode ${keyBits.bytes} in nft balance of account $user",
                  )
              }
          }
        }
        .takeWhile(_._1 === user)
        .map(_._2)
        .compile
        .toList
    yield tokenIds

  def getWeeklyTokenReceived[F[_]: Monad: StateRepository.RewardState](
      tokenList: List[TokenId],
      canonicalTimestamp: Instant,
      root: MerkleTrieState[(Instant, TokenId), DaoActivity],
  ): EitherT[F, String, DaoActivity] =
    val refInstants = getWeeklyRefTime(
      getLatestRewardInstantBefore(canonicalTimestamp),
    )

    val keyList = for
      refInstant <- refInstants
      tokenId    <- tokenList
    yield (refInstant, tokenId)

    keyList
      .traverse { (refInstant, tokenId) =>
        MerkleTrie
          .get[F, (Instant, TokenId), DaoActivity](
            (refInstant, tokenId).toBytes.bits,
          )
          .runA(root)
      }
      .map(_.flatten.combineAll)

  def getWeeklyTotalReceivedPoint[F[_]
    : Concurrent: StateRepository.RewardState](
      timestamp: Instant,
      root: MerkleTrieState[(Instant, TokenId), DaoActivity],
  ): EitherT[F, String, BigNat] =
    val to: Instant   = getLatestRewardInstantBefore(timestamp)
    val from: Instant = getLatestRewardInstantBefore(to)
    val timestampBits = from.toBytes.bits

    MerkleTrie
      .from[F, (Instant, TokenId), DaoActivity](timestampBits)
      .runA(root)
      .flatMap { stream =>
        stream
          .evalMap { case (keyBits, daoActivity) =>
            EitherT.fromEither[F] {
              ByteDecoder[(Instant, TokenId)]
                .decode(keyBits.bytes)
                .leftMap(_.msg)
                .flatMap { case DecodeResult((instant, tokenId), remainder) =>
                  if remainder.isEmpty then Right((instant, daoActivity))
                  else
                    Left(
                      s"Non-empty remainder: $remainder in token: ${keyBits.bytes}",
                    )
                },
            }
          }
          .takeWhile(_._1.compareTo(to) <= 0)
          .map(_._2)
          .map(daoActivityToMilliPoint)
          .foldMonoid
          .compile
          .toList
      }
      .map(_.head)
      .map(BigNat.fromBigInt(_).toOption.getOrElse(BigNat.Zero))

  def calculateTokenReceivedReward(
      numberOfDao: Int,
      tokenReceivedActivity: DaoActivity,
      totalReceivedMilliPoint: BigNat,
  ): BigNat =
    if numberOfDao <= 0 then BigNat.Zero
    else
      val limit = BigInt(125_000L) * 1000 * numberOfDao - 50_000L
      val tokenReceivedMilliPoint = BigNat
        .fromBigInt(daoActivityToMilliPoint(tokenReceivedActivity))
        .toOption
        .getOrElse(BigNat.Zero)
      val milliPoint: BigNat =
        if totalReceivedMilliPoint.toBigInt < limit then tokenReceivedMilliPoint
        else
          BigNat.unsafeFromBigInt(
            limit,
          ) * tokenReceivedMilliPoint / totalReceivedMilliPoint
      milliPoint * BigNat.unsafeFromBigInt(BigInt(10).pow(15))

  def getTotalRarityRewardAmount[F[_]
    : Concurrent: BlockRepository: TransactionRepository: StateRepository.TokenState](
      rewardAmount: Option[BigNat],
      daoAccount: Option[Account],
  ): EitherT[F, String, BigNat] = rewardAmount match
    case Some(amount) => EitherT.pure(amount)
    case None =>
      val targetAccount =
        daoAccount.getOrElse(Account(Utf8.unsafeFrom("DAO-M")))
      EitherT.right{
        StateReadService.getFreeBalance[F](targetAccount).map { balanceMap =>
          balanceMap
            .get(TokenDefinitionId(Utf8.unsafeFrom("LM")))
            .fold(BigNat.Zero)(_.totalAmount)
        }
      }

  def getUserRarityReward[F[_]
    : Concurrent: StateRepository.TokenState: StateRepository.RewardState](
      user: Account,
      state: GossipDomain.MerkleState.TokenMerkleState,
  ): EitherT[F, String, Map[Rarity, BigNat]] =
    getUserRarityItem[F](user, state).map(rarityItemsToRewardDetailMap)

  def getUserRarityItem[F[_]
    : Concurrent: StateRepository.TokenState: StateRepository.RewardState](
      user: Account,
      state: GossipDomain.MerkleState.TokenMerkleState,
  ): EitherT[F, String, List[(Rarity, BigNat)]] =
    for
      stream <- MerkleTrie
        .from[F, (Account, TokenId, Hash.Value[TransactionWithResult]), Unit](
          user.toBytes.bits,
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
            DecodeResult((_, tokenId, _), remainder) = decodeResult
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
          yield (nftState.rarity, nftState.weight)
        }
        .compile
        .toList
    yield result

  def rarityItemsToRewardDetailMap(
      items: List[(Rarity, BigNat)],
  ): Map[Rarity, BigNat] =
    items.groupMapReduce(_._1)(_._2)(_ + _)

  def getTotalRarityRewardValue[F[_]: Concurrent: StateRepository.TokenState](
      user: Account,
      state: GossipDomain.MerkleState.TokenMerkleState,
  ): EitherT[F, String, BigNat] =
    for
      keyBitsStream <- MerkleTrie
        .from[F, (TokenDefinitionId, Rarity, TokenId), Unit](BitVector.empty)
        .runA(state.rarityState)
      stream = keyBitsStream
        .evalMap { (keyBits, _) =>
          EitherT
            .fromEither {
              ByteDecoder[(TokenDefinitionId, Rarity, TokenId)]
                .decode(keyBits.bytes)
                .leftMap(_.msg)
                .flatMap {
                  case DecodeResult((defId, rarity, tokenId), remainder) =>
                    if remainder.isEmpty then Right((defId, rarity, tokenId))
                    else
                      Left(
                        s"Non-empty remainder: $remainder in token: ${keyBits.bytes}",
                      )
                }
            }
        }
      ansList <- stream
        .evalMapAccumulate((Option.empty[TokenDefinition], BigNat.Zero)) {
          case ((Some(tokenDef), acc), (defId, rarity, _))
              if tokenDef.id === defId =>
            val weight = getWeightfromTokenDef(tokenDef, rarity)
            EitherT.rightT[F, String] {
              ((Some(tokenDef), weight + acc), ())
            }
          case ((_, acc), (defId, rarity, _)) =>
            for
              tokenDefOption <- MerkleTrie
                .get[F, TokenDefinitionId, TokenDefinition](defId.toBytes.bits)
                .runA(state.tokenDefinitionState)
              tokenDef <- EitherT.fromOption(
                tokenDefOption,
                s"TokenDefinition not found: $defId",
              )
            yield
              val weight = getWeightfromTokenDef(tokenDef, rarity)
              ((Some(tokenDef), weight + acc), ())
        }
        .last
        .compile
        .toList
    yield ansList.flatten.headOption.fold(BigNat.Zero)(_._1._2)

  def getWeightfromTokenDef(
      tokenDef: TokenDefinition,
      rarity: Rarity,
  ): BigNat = {
    for
      nftInfo <- tokenDef.nftInfo
      weight  <- nftInfo.rarity.get(rarity)
    yield weight
  }.getOrElse(BigNat.Zero)

  def calculateUserRarityRewardValue(
      totalRarityRewardAmount: BigNat,
      totalNumberOfDao: Int,
      userRarityRewardItems: List[(Rarity, BigNat)],
      totalRarityRewardValue: BigNat,
  ): BigNat =
    val limit = BigNat.unsafeFromLong(250_000L * totalNumberOfDao)
    val totalAmount = BigNat.min(totalRarityRewardAmount, limit)
    val userRarityReward =
      userRarityRewardItems.map(_._2).foldLeft(BigNat.Zero)(BigNat.add)
      
    totalAmount * userRarityReward / totalRarityRewardValue
    
  def isModerator[F[_]: Concurrent: StateRepository.RewardState](
      user: Account,
      root: MerkleTrieState[GroupId, DaoInfo],
  ): EitherT[F, String, Boolean] =
    for
      stream <- MerkleTrie
        .from[F, GroupId, DaoInfo](BitVector.empty)
        .runA(root)
      ansList <- stream
        .exists { case (_, daoInfo) => daoInfo.moderators.contains(user) }
        .compile
        .toList
    yield ansList.head
