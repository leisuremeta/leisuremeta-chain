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
import api.model.token.TokenId
import api.model.TransactionWithResult.ops.*
import lib.merkle.{MerkleTrie, MerkleTrieState}
import lib.codec.byte.{ByteDecoder, DecodeResult}
import lib.codec.byte.ByteEncoder.ops.*
import lib.crypto.Hash
import lib.crypto.Hash.ops.*
import lib.datatype.{BigNat, Utf8}
import repository.{StateRepository, TransactionRepository}
import repository.StateRepository.given
import io.leisuremeta.chain.node.service.StateReadService

trait UpdateStateWithRewardTx:

  given updateStateWithRewardTx[F[_]: Concurrent: StateRepository.GroupState: StateRepository.RewardState]
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
              moderators = rd.moderators
            )
            daoState <- MerkleTrie
              .put[F, GroupId, DaoInfo](rd.groupId.toBytes.bits, daoInfo)
              .runS(ms.reward.daoState)
          yield (
            ms.copy(reward =
              ms.reward.copy(daoState = daoState),
            ),
            TransactionWithResult(Signed(sig, rd), None),
          )
        case ud: Transaction.RewardTx.UpdateDao => ???
        case ra: Transaction.RewardTx.RecordActivity =>
          for
            userActivityState <- ra.userActivity.toList.traverse{ case (account, activity) =>
              val canonicalInstant = ra.timestamp
                .atZone(ZoneId.of("Asia/Seoul"))
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant()
              val keyBits = (canonicalInstant, account).toBytes.bits
              for
                activityOption <- MerkleTrie.get[F, (Instant, Account), DaoActivity](keyBits)
                _ <- MerkleTrie.remove[F, (Instant, Account), DaoActivity](keyBits)
                activity1 = activityOption.fold(activity)(Monoid[DaoActivity].combine(_, activity))
                _ <- MerkleTrie.put[F, (Instant, Account), DaoActivity](keyBits, activity1)
              yield ()
            }.runS(ms.reward.userActivityState)
            tokenReceivedState <- ra.tokenReceived.toList.traverse{ case (tokenId, activity) =>
              MerkleTrie.put[F, (Instant, TokenId), DaoActivity](
                (ra.timestamp, tokenId).toBytes.bits,
                activity
              )
            }.runS(ms.reward.tokenReceivedState)
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
        case rms: Transaction.RewardTx.RemoveStaking => ???

end UpdateStateWithRewardTx
